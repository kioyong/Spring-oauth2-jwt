package com.yong.security;

import com.yong.security.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;

/**
 * @author  LiangYong
 * @createdDate 2017/10/8.
 */
@Configuration
@ConfigurationProperties
public class Oauth2ServerConfig {
    private static final String RESOURCE_ID = "yong";

    @Value("${yong.oauth.enable}")
    private static Boolean enable_auth;

    @Configuration
    @EnableWebSecurity
    @Order(-1)
    protected static class WebSecurityConfig extends WebSecurityConfigurerAdapter  {

        @Autowired
        private UserDetailsService userDetailsService;

        @Override
        public void configure(@Autowired AuthenticationManagerBuilder auth) throws Exception {
            //注入校验登录用户账号密码的service
            auth.userDetailsService(this.userDetailsService)
                    .passwordEncoder(new BCryptPasswordEncoder());
//            InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
//            manager.createUser(User.withUsername("user").password("password").roles("USER").build());
//            manager.createUser(User.withUsername("admin").password("password").roles("USER","ADMIN").build());
//            return manager;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            /**
             * **/
            http.addFilterBefore(new CorsFilter(), ChannelProcessingFilter.class);
            http.requestMatchers().antMatchers(HttpMethod.OPTIONS, "/oauth/**")
                    .and().authorizeRequests().anyRequest().permitAll()
                    .and().csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Bean
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }
    }

    @Configuration
    @EnableAuthorizationServer
    protected class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
        @Autowired
        private AuthenticationManager authenticationManager;

        @Bean
        public JwtAccessTokenConverter accessTokenConverter() {
            return new JwtAccessTokenConverter();
        }

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints
                .accessTokenConverter(accessTokenConverter())
                .authenticationManager(this.authenticationManager);
        }
        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.inMemory()
                .withClient("yong")
                .secret("passw0rd")
                .authorities("ROLE_TRUSTED_CLIENT")
                .accessTokenValiditySeconds(3600)
                .authorizedGrantTypes("password","refresh_token")
                .scopes("read", "write")
                .autoApprove("read", "write");
        }

    }

    @Configuration
    @EnableResourceServer
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
        private ClientDetailsService clientDetailsService;

        @Bean
        public JwtAccessTokenConverter accessTokenConverter() {
            return new JwtAccessTokenConverter();
        }
        @Bean
        public TokenStore tokenStore() {
            return new JwtTokenStore(accessTokenConverter());
        }
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {

            final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
            defaultTokenServices.setTokenStore(tokenStore());
            defaultTokenServices.setTokenEnhancer(accessTokenConverter());
            defaultTokenServices.setClientDetailsService(clientDetailsService);
            resources.resourceId(RESOURCE_ID).tokenServices(defaultTokenServices);
        }
        @Override
        public void configure(HttpSecurity http) throws Exception {
            if(enable_auth) {
                http.authorizeRequests()
                        .antMatchers(HttpMethod.GET, "/", "/*.html", "/**/*.css", "/**/*.js", "/**/*.png").permitAll()
                        .antMatchers("/user/register", "/index", "/v2/api-docs", "/swagger-resources/**").permitAll()
                        .anyRequest().authenticated().and().csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }else {
                http.authorizeRequests()
                        .anyRequest().permitAll();//取消安全校验，当测试其他function且不跟权限相关的时候可以使用这里
            }
        }
    }

}
