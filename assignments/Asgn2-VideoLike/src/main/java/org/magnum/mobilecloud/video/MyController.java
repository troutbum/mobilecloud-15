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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
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
	
//	methods used in Assignment 1 superceded by JPA implementation
//	
//	// An in-memory database (HashMap object) that the servlet uses to store the
//	// videos that are sent to it by clients
//
//	// One way to generate a unique ID for each video is to use an AtomicLong:
//	private static final AtomicLong currentId = new AtomicLong(0L);
//	
//	// Create database object for videos
//	private Map<Long,Video> videos = new HashMap<Long, Video>();
//
//	// Method for saving video to database
//	public Video save(Video entity) {
//		checkAndSetId(entity);
//		videos.put(entity.getId(), entity);
//		return entity;
//	}
//	// Method to check if ID exists, if not generate one
//	private void checkAndSetId(Video entity) {
//		if(entity.getId() == 0){
//			entity.setId(currentId.incrementAndGet());
//		}
//	}
//
	// Method to generate a data url for a video
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
	
	
//	methods used in Assignment 1 superceded by JPA implementation
//
	// Controller METHOD1 - Receives GET requests to VIDEO_SVC_PATH
	// and returns the current list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
//	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
//	public @ResponseBody Collection<Video> getVideoList(){
//		return videos.values();
//	}
	
	// Receives GET requests to /video and returns the current
		// list of videos in memory. Spring automatically converts
		// the list of videos to JSON because of the @ResponseBody
		// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return Lists.newArrayList(videos.findAll());
	}
	
//	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
//	public @ResponseBody Collection<Video> getVideoList(){
//		return (Collection<Video>) videos.findAll();
//	}
	
	
	// Receives GET requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(value=VideoSvcApi.TITLE_PARAMETER) String title
			){
		return videos.findByName(title);
	}
	
	
	// Controller METHOD2 - Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to convert the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.

//	methods used in Assignment 1 superceded by JPA implementation
//	
//	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
//	public @ResponseBody Video addVideo(@RequestBody Video v){
//		save(v);
//		// add dataUrl to object so client can upload file here
//		v.setDataUrl(createDataUrl(v.getId()));	
//		return v;
//	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		videos.save(v);
		// add dataUrl to object so client can upload file here
		v.setUrl(createDataUrl(v.getId()));	
//		System.out.println(v.getUrl());
		return v;
	}	
	
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
