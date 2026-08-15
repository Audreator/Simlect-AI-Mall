package com.simlect.component;

import jodd.bean.BeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeanException {
        if (SpringContext.applicationContext == null){
            SpringContext.applicationContext = applicationContext;
        }
    }

    public static Object getBean(String beanName){
        return SpringContext.applicationContext.getBean(beanName);
    }
}
