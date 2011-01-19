/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.duradmin.spaces.controller;

import org.apache.commons.httpclient.HttpStatus;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStore.AccessType;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.StoreCaller;
import org.duracloud.common.util.ExtendedIteratorCounterThread;
import org.duracloud.controller.AbstractRestController;
import org.duracloud.duradmin.domain.Space;
import org.duracloud.duradmin.domain.SpaceMetadata;
import org.duracloud.duradmin.util.MetadataUtils;
import org.duracloud.duradmin.util.SpaceUtil;
import org.duracloud.error.ContentStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class SpaceController extends  AbstractRestController<Space> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

	private ContentStoreManager contentStoreManager;
	
	public SpaceController(){
		super(null);
		setValidator(new Validator(){
			@SuppressWarnings("unchecked")
			@Override
			public boolean supports(Class clazz) {
				return clazz == Space.class;
			}
			
			@Override
			public void validate(Object target, Errors errors) {
				Space command = (Space)target;

		        if (!StringUtils.hasText(command.getStoreId())) {
		            errors.rejectValue("storeId","required");
		        }

				if (!StringUtils.hasText(command.getSpaceId())) {
		            errors.rejectValue("spaceId","required");
		        }
			}
		});

	}
    
    public ContentStoreManager getContentStoreManager() {
		return contentStoreManager;
	}

	public void setContentStoreManager(ContentStoreManager contentStoreManager) {
		this.contentStoreManager = contentStoreManager;
	}


	
	

	@Override
	protected ModelAndView get(HttpServletRequest request,
			HttpServletResponse response, Space space,
			BindException errors) throws Exception {
		try{
			String prefix = request.getParameter("prefix");
			if(prefix != null){
				prefix = ("".equals(prefix.trim())?null:prefix);
			}
			String marker = request.getParameter("marker");
			org.duracloud.domain.Space cloudSpace = 
			contentStoreManager.getContentStore(space.getStoreId()).getSpace(space.getSpaceId(), prefix, 200, marker);
			SpaceUtil.populateSpace(space, cloudSpace);
			populateSpaceCount(space, request);
			return createModel(space);
		}catch(ContentStoreException ex){
			ex.printStackTrace();
			response.setStatus(HttpStatus.SC_NOT_FOUND);
			return createModel(null);
		}
	}
	
	
	private void populateSpaceCount(Space space, HttpServletRequest request) throws Exception{
		String countStr = space.getMetadata().getCount();
		if(countStr.endsWith("+")){
			setItemCount(space, request);
		}else{
			space.setItemCount(Long.valueOf(space.getMetadata().getCount()));
		}
	}

	private void setItemCount(final Space space, HttpServletRequest request) throws ContentStoreException{
		String key = space.getStoreId() + "/" + space.getSpaceId() + "/itemCountListener";
		ItemCounter listener = (ItemCounter)request.getSession().getAttribute(key);
		space.setItemCount(new Long(-1));
        if(listener != null){
            if(listener.isCountComplete()) {
                space.setItemCount(listener.getCount());
                request.getSession().removeAttribute(key);
            } else {
                SpaceMetadata metadata = space.getMetadata();
                long interCount = listener.getIntermediaryCount();
                if(interCount % 1000 != 0) {
                    interCount += 1;
                }
                metadata.setCount(String.valueOf(interCount) + "+");
                space.setMetadata(metadata);
            }
		}else{
			request.getSession().setAttribute(key, listener = new ItemCounter());
			final ContentStore contentStore = contentStoreManager.getContentStore(space.getStoreId());
			StoreCaller<Iterator<String>> caller = new StoreCaller<Iterator<String>>() {
	            protected Iterator<String> doCall() throws ContentStoreException {
	            	return contentStore.getSpaceContents(space.getSpaceId());
	            }
	            public String getLogMessage() {
	                return "Error calling contentStore.getSpaceContents() for: " +
	                   space.getSpaceId();
	            }
	        };

	        new Thread(new ExtendedIteratorCounterThread(caller.call(), listener)).start();
		}
	}

	@Override
	protected ModelAndView put(HttpServletRequest request,
			HttpServletResponse response, Space space,
			BindException errors) throws Exception {
		String spaceId = space.getSpaceId();
        ContentStore contentStore = getContentStore(space);
        
        String method = request.getParameter("method");
        if("changeAccess".equals(method)){
            String access = space.getAccess();
            if(access !=null){
                contentStore.setSpaceAccess(spaceId, AccessType.valueOf(access));
            }
            
            Space newSpace = new Space();
            SpaceUtil.populateSpace(newSpace, contentStore.getSpace(spaceId,
                    null,
                    0,
                    null));            
            
            return createModel(newSpace);
        }else{ 
        	Map<String,String> metadata  = contentStore.getSpaceMetadata(spaceId);
        	MetadataUtils.handle(method, "space ["+spaceId+"]",  metadata, request);
        	contentStore.setSpaceMetadata(spaceId, metadata);
            Space newSpace = new Space();
            SpaceUtil.populateSpace(newSpace, contentStore.getSpace(spaceId,
                    null,
                    0,
                    null));
    		return createModel(newSpace);

        }
       
	}

	protected ModelAndView post(HttpServletRequest request,
			HttpServletResponse response, Space space,
			BindException errors) throws Exception {
			String spaceId = space.getSpaceId();
	        ContentStore contentStore = getContentStore(space);
	        contentStore.createSpace(spaceId, null);
	        contentStore.setSpaceAccess(spaceId, AccessType.valueOf(space
	                .getAccess()));
	        SpaceUtil.populateSpace(space, contentStore.getSpace(spaceId,
	                                                             null,
	                                                             0,
	                                                             null));
			return createModel(space);
	}

	
	protected ModelAndView delete(HttpServletRequest request,
			HttpServletResponse response, Space space,
			BindException errors) throws Exception {
		String spaceId = space.getSpaceId();
        ContentStore contentStore = getContentStore(space);
        contentStore.deleteSpace(spaceId);
        return createModel(space);
	}

	private ModelAndView createModel(Space space){
        return new ModelAndView("jsonView", "space",space);
	}
	
	protected ContentStore getContentStore(Space space) throws ContentStoreException{
		return contentStoreManager.getContentStore(space.getStoreId());
	}


}
