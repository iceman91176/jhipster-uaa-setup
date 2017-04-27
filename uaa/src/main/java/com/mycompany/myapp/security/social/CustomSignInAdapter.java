package com.mycompany.myapp.security.social;

import javax.inject.Inject;

import org.springframework.social.connect.web.SignInAdapter;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mycompany.myapp.config.JHipsterProperties;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.CustomAuditEventRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.social.connect.Connection;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.UriUtils;


public class CustomSignInAdapter implements SignInAdapter{
	
	private final Logger log = LoggerFactory.getLogger(CustomSignInAdapter.class);
	
    @Inject
    private UserDetailsService userDetailsService;
    
    @Inject
    private UserRepository userRepository;

    @Inject
    private JHipsterProperties jHipsterProperties;
    
    @Inject
    private CustomAuditEventRepository persistenceAuditEventRepository;
    
    public static final String REDIRECT_PATH_BASE = "http://linux-dev-01:8080/#/";
    public static final String FIELD_TOKEN = "access_token";
    public static final String FIELD_EXPIRATION_SECS = "expires_in";
    
    @Inject
    private AuthorizationServerTokenServices authTokenServices;
    
    @Override
    public String signIn(String userId, Connection<?> connection, NativeWebRequest request) {
    	log.debug("Social user authenticated: " + userId + ", generating and sending local auth");
        UserDetails user = userDetailsService.loadUserByUsername(userId);
        
        OAuth2Authentication authentication = convertAuthentication(user);
        OAuth2AccessToken accessToken = authTokenServices.createAccessToken(authentication);
        log.debug(accessToken.toString());
        
        log.debug("Updating userprofile");
        Optional<User> loadeduser =userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin());
        if (loadeduser.isPresent()) {
        	User myUser = loadeduser.get();
        	myUser.setEmail(connection.fetchUserProfile().getEmail());
        	myUser.setFirstName(connection.fetchUserProfile().getFirstName());
        	myUser.setLastName(connection.fetchUserProfile().getLastName());
        	userRepository.save(myUser);
        	log.info("Updated userprofile {}", myUser);
        }

        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
        	ipAddress = (((ServletWebRequest) request).getRequest()).getRemoteAddr();
        }
        
        String principal = SecurityUtils.getCurrentUserLogin();
		Map<String, Object> eventData = new HashMap<String, Object>();
		eventData.put("remoteAddress", ipAddress);
		AuditEvent event = new AuditEvent(principal, "AUTHENTICATION_SUCCESS", eventData);
    	
    	persistenceAuditEventRepository.add(event);
    	
    	String redirectUrl = new StringBuilder(REDIRECT_PATH_BASE)
                .append("?").append(FIELD_TOKEN).append("=")
                .append(encode(accessToken.getValue()))
                .append("&").append(FIELD_EXPIRATION_SECS).append("=")
                .append(accessToken.getExpiresIn())
                .toString();
        log.debug("Sending redirection to " + redirectUrl);
    	return redirectUrl;
    }
    
    
    private OAuth2Authentication convertAuthentication(UserDetails userDetails) {
        Set<String> scope = new HashSet<String>();
        scope.add("openid");
		OAuth2Request request = new OAuth2Request(null, "web_app", null, true, scope , null, null, null, null);
        return new OAuth2Authentication(request, new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
    }
    
    private String encode(String in){
        String res = in;
        try {
            res = UriUtils.encode(in, "UTF-8");
        } catch(UnsupportedEncodingException e){
            log.error("ERROR: unsupported encoding: " + "UTF-8", e);
        }
        return res;
    }
    

}
