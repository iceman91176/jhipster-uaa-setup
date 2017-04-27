package com.mycompany.myapp.security.social;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;

@Component
public class SignupHandler implements ConnectionSignUp
{

	private final Logger log = LoggerFactory.getLogger(SignupHandler.class);

	@Autowired
    private AuthorityRepository authorityRepository;

	@Autowired
    private PasswordEncoder passwordEncoder;

	@Autowired
    private UserRepository userRepository;
    
	@Override
	@Transactional
	public String execute(final Connection<?> connection) {
		String langKey = "de";
		
        if (connection == null) {
            log.error("Cannot create social user because connection is null");
            throw new IllegalArgumentException("Connection cannot be null");
        }
        UserProfile userProfile = connection.fetchUserProfile();
        log.debug("Profile {}",userProfile);
        String providerId = connection.getKey().getProviderId();
        User user = createUserIfNotExist(userProfile, langKey, providerId);
        //mailService.sendSocialRegistrationValidationEmail(user, providerId);
		return user.getLogin();
	}

    private User createUserIfNotExist(UserProfile userProfile, String langKey, String providerId) {
        String email = userProfile.getEmail();
        String userName = userProfile.getUsername();
        if (StringUtils.isBlank(email) && StringUtils.isBlank(userName)) {
            log.error("Cannot create social user because email and login are null");
            throw new IllegalArgumentException("Email and login cannot be null");
        }
        
        String login = getLoginDependingOnProviderId(userProfile, providerId);
        if (StringUtils.isBlank(email) && userRepository.findOneByLogin(login).isPresent()) {
            log.error("Cannot create social user because email is null and login already exist, login -> {}", userName);
            throw new IllegalArgumentException("Email cannot be null with an existing login");
        }

        Optional<User> user = userRepository.findOneByLogin(login);
        if (user.isPresent()) {
			log.info("User already exist associate the connection to this account and update userprofile");
            User myUser = user.get();
            myUser.setEmail(email);
            myUser.setFirstName(userProfile.getFirstName());
            myUser.setLastName(userProfile.getLastName());
            return userRepository.save(myUser);
        }

        String encryptedPassword = passwordEncoder.encode(RandomStringUtils.random(10));
        Set<Authority> authorities = new HashSet<>(1);
        authorities.add(authorityRepository.findOne("ROLE_USER"));

        User newUser = new User();
        newUser.setLogin(login);
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userProfile.getFirstName());
        newUser.setLastName(userProfile.getLastName());
        newUser.setEmail(email);
        newUser.setActivated(true);
        //newUser.setExternal(true);
        newUser.setLangKey(langKey);
        newUser.setAuthorities(authorities);

        return userRepository.save(newUser);
    }	
	
    /**
     * @return login if provider manage a login like Twitter or Github otherwise email address.
     *         Because provider like Google or Facebook didn't provide login or login like "12099388847393"
     */
    private String getLoginDependingOnProviderId(UserProfile userProfile, String providerId) {
        switch (providerId) {
            case "twitter":
                return userProfile.getUsername().toLowerCase();
            case "wso2is":
            	return userProfile.getUsername().toLowerCase();	
            default:
                return userProfile.getEmail();
        }
    }

}
