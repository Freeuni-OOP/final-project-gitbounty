package org.gitbounty.gitbountybackend.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Gives JPA-instantiated objects (entity listeners) a way to reach Spring beans.
 *
 * JPA lifecycle callbacks (@PrePersist, @PreRemove, etc.) run on objects that Hibernate
 * instantiates directly via a no-arg constructor, never through Spring - so normal
 * constructor/field injection isn't available to them. This codebase has no AspectJ
 * load-time weaving (no @Configurable support) and no prior static-context-holder
 * precedent, so this is the minimal, standard workaround: a singleton bean that captures
 * the ApplicationContext at startup and exposes it for lookup at call time.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}
