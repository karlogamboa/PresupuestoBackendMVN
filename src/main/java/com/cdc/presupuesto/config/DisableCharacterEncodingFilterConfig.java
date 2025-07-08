package com.cdc.presupuesto.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.RequestContextFilter;

@Configuration
@Profile("lambda")
@ConditionalOnWebApplication
public class DisableCharacterEncodingFilterConfig {
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> disableCharacterEncodingFilter(CharacterEncodingFilter filter) {
        FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<FormContentFilter> disableFormContentFilter(FormContentFilter filter) {
        FilterRegistrationBean<FormContentFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestContextFilter> disableRequestContextFilter() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(new RequestContextFilter());
        registration.setEnabled(false);
        return registration;
    }
}
