package com.sap.loves.docProcess.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**************************************************************
 * 
 * @author I304296
 * Basic Authentication Configuration
 * 
 * 
 *************************************************************/
/*
@Configuration
@EnableWebSecurity
public class ConfigRestSecurity extends WebSecurityConfigurerAdapter{
	
	private static String REALM="DPS_REALM";
	
    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser("LovesDPSUser").password("{noop}Password123").roles("ADMIN");
    	
//    	PasswordEncoder encoder =
//    		     PasswordEncoderFactories.createDelegatingPasswordEncoder();
//    	
//    	UserDetails user = User.withUsername("dpsuser")
//                .password(encoder.encode("Password"))
//                .roles("ADMIN").build();
    	
    }
    
//    @Bean
//    public UserDetailsService userDetailsService() {
//
//        User.UserBuilder users = User.withDefaultPasswordEncoder();
//        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
//        manager.createUser(users.username("user").password("password").roles("USER").build());
//        manager.createUser(users.username("dpsuser").password("Password").roles("ADMIN").build());
//        return manager;
//
//    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {

    	//Basic Auth for ADMIN role
        http.csrf().disable()
        .authorizeRequests()
        .antMatchers("/rest/**").hasRole("ADMIN")
        .and().httpBasic().realmName(REALM).authenticationEntryPoint(getBasicAuthEntryPoint())
        .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);//We don't need sessions to be created.
      

    }
    
    @Bean
    public CustomBasicAuthenticationEntryPoint getBasicAuthEntryPoint(){
        return new CustomBasicAuthenticationEntryPoint();
    }
    // To allow Pre-flight [OPTIONS] request from browser 
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");
    }
    
//    @Bean
//    public static NoOpPasswordEncoder passwordEncoder() {
//    	return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
//    }
    
}

*/