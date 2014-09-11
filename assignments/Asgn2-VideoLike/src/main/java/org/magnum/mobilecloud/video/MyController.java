/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Lists;

@Controller
public class MyController {
	
	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	// Our VideoRepository extends CrudRepository<Video, Long>
	
	@Autowired
	private VideoRepository videos;
		
	// GET /video 
	// Requests to VIDEO_SVC_PATH and returns the current list of 
	// videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return Lists.newArrayList(videos.findAll());
	}
	
	// GET /video/search/findByName?title={title}
	// Requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client or an empty
	// list if none are found.
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(value=VideoSvcApi.TITLE_PARAMETER) String title
			){
		return videos.findByName(title);
	}
	
	// GET /video/search/findByDurationLessThan?duration={duration}
	// Returns a list of videos whose durations are less than the given parameter 
	// or an empty list if none are found.
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDuration(
			// Tell Spring to use the "duration" parameter in the HTTP request's query
			// string as the value for the duration method parameter
			@RequestParam(value=VideoSvcApi.DURATION_PARAMETER) long maxduration
			){
		return videos.findByDurationLessThan(maxduration);
	}
		
	// GET /video/{id}
	// GET requests with path variable {id} to serve up video metadata from the server	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method = RequestMethod.GET)
	public @ResponseBody Video getVideoById(
			@PathVariable("id") long id,
			HttpServletResponse response) {

		Video v = videos.findOne(id);
		if (v == null) {
			try {
				response.sendError(404);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return videos.findOne(id);
	}	
	
	// POST /video 
	// Requests to VIDEO_SVC_PATH converts the HTTP request body, which should contain JSON, 
	// into a Video object before adding it repository. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to convert the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		Video vhandle = videos.save(v);
		// use returned instance "vhandle" from save() to get assigned ID
		// then add dataUrl to object so client can upload file here	
		v.setUrl(createDataUrl(vhandle.getId()));			
		// update repository with URL
		videos.save(v);
		return v;
	}	
	
	// Method to generate a data URL to store a video
	private String createDataUrl(long videoId){
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}
	//	Figure out the address of your server 
	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = 
				"http://"+request.getServerName() 
				+ ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		return base;
	}
	
//	POST /video/{id}/like
//	Allows a user to like a video. Returns 200 Ok on success, 404 if the video is not found, 
//	or 400 if the user has already liked the video.
//	The service should should keep track of which users have liked a video and prevent a user from liking a video twice. 
//  If a user tries to like a video a second time, the operation should fail and return 400 Bad Request.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method = RequestMethod.POST)
	public ResponseEntity<Void> likeVideo(
			@PathVariable("id") long id,
			Principal p) {

		// Check if id exists in repository
		if (videos.exists(id) == false) {
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		
		// Get username of current login account
		String username = p.getName();	
		// Get video and list of usernames that like
		Video v = videos.findOne(id);
		List<String> likesUsernames = v.getLikesUsernames();  
		
		// Checks if the user has already liked the video (returns 400 Bad Request)
		if (likesUsernames.contains(username)) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		} 
		
		// keep track of users have liked a video
		likesUsernames.add(username);
		v.setLikesUsernames(likesUsernames);
		v.setLikes(likesUsernames.size());
		videos.save(v);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}		
	
//	POST /video/{id}/unlike
//	Allows a user to unlike a video that he/she previously liked. Returns 200 OK on success, 
//	404 if the video is not found, and a 400 if the user has not previously liked the specified video.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method = RequestMethod.POST)
	public ResponseEntity<Void> unlikeVideo(
			@PathVariable("id") long id,
			Principal p) {

		// Check if id exists in repository
		if (videos.exists(id) == false) {
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		
		// Get username of current login account
		String username = p.getName();
		// Get video and list of usernames that like
		Video v = videos.findOne(id);
		List<String> likesUsernames = v.getLikesUsernames();  
		
		// Checks if the user has not previously liked the video (returns 400 Bad Request)
		if (!likesUsernames.contains(username)) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		} 
		
		// keep track of users have liked a video
		likesUsernames.remove(username);
		v.setLikesUsernames(likesUsernames);
		v.setLikes(likesUsernames.size());
		videos.save(v);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	//	GET /video/{id}/likedby
	//	Returns a list of the string usernames of the users that have liked the specified video. 
	//	If the video is not found, a 404 error should be generated.	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method = RequestMethod.GET)
	public @ResponseBody List<String> getUsersWhoLikedVideo(
			@PathVariable("id") long id,
			HttpServletResponse response) throws IOException {

		// Check if id exists in repository
		if (videos.exists(id) == false) {
			response.sendError(404);
			return null;
		}
		
		// Get video and list of usernames that like
		Video v = videos.findOne(id);
		List<String> likesUsernames = v.getLikesUsernames();  

		return likesUsernames;
	}
	
//	
//  Previous code from Assignment 1 use to store/retrieve binary video files
//
//	// Controller METHOD3 - Receives POST requests
//	// to save client's video data to a file on the server
//		
//	private VideoFileManager videoDataMgr;
//	
//	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST )
//	public @ResponseBody VideoStatus setVideoData(	
//			@PathVariable("id") long id,
//			@RequestParam("data") MultipartFile videoData,		
//		    HttpServletResponse response) 
//			throws IOException {
//
//		try {
//			videoDataMgr = VideoFileManager.get();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	
//		// lookup Video object using ID sent by client
//		// and then store binary video data into the file system
//		Video v =(Video)videos.get(id);	
//		try {
//			videoDataMgr.saveVideoData(v, videoData.getInputStream());
//		} catch (Exception ee) {
//			response.sendError(404);
//		}
//		
//		return new VideoStatus(VideoState.READY);
//   }
//
//	// Controller METHOD4 - Receives GET requests
//	// to serve up binary video data from the server
//	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET )
//	public void getVideoData(
//			@PathVariable("id") long id,
//			HttpServletResponse response)
//		    throws IOException {
//		
//		try {
//			Video v =(Video)videos.get(id);
//			videoDataMgr.copyVideoData(v, response.getOutputStream());
//		} catch (Exception ee) {
//			response.sendError(404);
//		}
//		
//		return;	
//	}	
	
}
