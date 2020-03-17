package io.mixeway.fortifyscaapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${allowed.users}")
    private String commonNames;

    @Profile("dev")
    @Configuration
    public static class DevSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            System.out.println("Enabling development mode");
            http.csrf().disable();
            http
                    .authorizeRequests()
                    .anyRequest()
                    .permitAll();

        }
    }
    @Profile("!dev")
    @Order(1)
    @Configuration
    public static class ProdSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            System.out.println("Enabling production mode");
            http.csrf().disable();
            http
                    .authorizeRequests()
                    .anyRequest()
                    .authenticated()
                    .and()
                    .x509()
                    .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                    .userDetailsService(userDetailsService());
        }
    }


    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                if (verifyCN(username)) {
                    return new User(username, "", AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
                } else
                    throw new UsernameNotFoundException("User not found!");
            }
        };
    }
    private boolean verifyCN(String cn){
        List<String> allowedCNs =
                Stream.of(commonNames.split(","))
                        .collect(Collectors.toList());
        return allowedCNs.contains(cn);
    }
}
