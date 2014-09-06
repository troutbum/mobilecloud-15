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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MyController {
	
	// An in-memory database (HashMap object) that the servlet uses to store the
	// videos that are sent to it by clients

	// One way to generate a unique ID for each video is to use an AtomicLong:
	private static final AtomicLong currentId = new AtomicLong(0L);
	
	// Create database object for videos
	private Map<Long,Video> videos = new HashMap<Long, Video>();

	// Method for saving video to database
	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}
	// Method to check if ID exists, if not generate one
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}

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
	
	// The VIDEO_SVC_PATH is set to "/video" in the VideoSvcApi interface.
	public static final String VIDEO_SVC_PATH = "/video";
	
	// Controller METHOD1 - Receives GET requests to VIDEO_SVC_PATH
	// and returns the current list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
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
	
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		save(v);
		// add dataUrl to object so client can upload file here
		v.setDataUrl(createDataUrl(v.getId()));	
		return v;
	}
	
	// Controller METHOD3 - Receives POST requests
	// to save client's video data to a file on the server
		
	private VideoFileManager videoDataMgr;
	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST )
	public @ResponseBody VideoStatus setVideoData(	
			@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData,		
		    HttpServletResponse response) 
			throws IOException {

		try {
			videoDataMgr = VideoFileManager.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		// lookup Video object using ID sent by client
		// and then store binary video data into the file system
		Video v =(Video)videos.get(id);	
		try {
			videoDataMgr.saveVideoData(v, videoData.getInputStream());
		} catch (Exception ee) {
			response.sendError(404);
		}
		
		return new VideoStatus(VideoState.READY);
   }

	// Controller METHOD4 - Receives GET requests
	// to serve up binary video data from the server
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET )
	public void getVideoData(
			@PathVariable("id") long id,
			HttpServletResponse response)
		    throws IOException {
		
		try {
			Video v =(Video)videos.get(id);
			videoDataMgr.copyVideoData(v, response.getOutputStream());
		} catch (Exception ee) {
			response.sendError(404);
		}
		
		return;	
	}	
	
}
