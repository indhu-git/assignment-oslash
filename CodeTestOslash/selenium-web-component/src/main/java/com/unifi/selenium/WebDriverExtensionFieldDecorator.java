package com.unifi.selenium;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindAll;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultFieldDecorator;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

public class WebDriverExtensionFieldDecorator extends DefaultFieldDecorator {

    private final WebDriver driver;
    private final WebComponentFactory webComponentFactory;
    private final WebComponentListFactory webComponentListFactory;
    private ParameterizedType genericTypeArguments;

    public void setGenericTypeArguments(ParameterizedType genericTypeArguments) {
        this.genericTypeArguments = genericTypeArguments;
    }

    public WebDriverExtensionFieldDecorator(final WebDriver driver) {
        super(new WebDriverExtensionElementLocatorFactory(driver, driver));
        this.driver = driver;
        this.webComponentFactory = new DefaultWebComponentFactory(driver);
        this.webComponentListFactory = new DefaultWebComponentListFactory(webComponentFactory);
        this.genericTypeArguments = null;
    }

    public WebDriverExtensionFieldDecorator(final SearchContext searchContext, final WebDriver driver) {
        super(new WebDriverExtensionElementLocatorFactory(searchContext, driver));
        this.driver = driver;
        this.webComponentFactory = new DefaultWebComponentFactory(driver);
        this.webComponentListFactory = new DefaultWebComponentListFactory(webComponentFactory);
        this.genericTypeArguments = null;
    }

    public WebDriverExtensionFieldDecorator(final SearchContext searchContext, final WebDriver driver, final ParameterizedType genericTypeArguments) {
        super(new WebDriverExtensionElementLocatorFactory(searchContext, driver));
        this.driver = driver;
        this.webComponentFactory = new DefaultWebComponentFactory(driver);
        this.webComponentListFactory = new DefaultWebComponentListFactory(webComponentFactory);
        this.genericTypeArguments = genericTypeArguments;
    }

    @Override
    public Object decorate(ClassLoader loader, Field field) {
        try {
            if (isDecoratableWebComponent(field)) {
                if (field.getGenericType() instanceof TypeVariable) {
                    return decorateWebComponent(loader, field, genericTypeArguments);
                } else if (field.getGenericType() instanceof ParameterizedType) {
                    return decorateWebComponent(loader, field, (ParameterizedType) field.getGenericType());
                } else {
                    return decorateWebComponent(loader, field, null);
                }
            }
            if (isDecoratableWebComponentList(field)) {
                Type listType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                if (listType instanceof TypeVariable) {
                    return decorateWebComponentList(loader, field, genericTypeArguments);
                } else if (listType instanceof ParameterizedType) {
                    return decorateWebComponentList(loader, field, (ParameterizedType) listType);
                } else {
                    return decorateWebComponentList(loader, field, null);
                }
            }
            if ("wrappedWebElement".equals(field.getName())) {
                return null;
            }
            if ("delegateWebElement".equals(field.getName())) {
                return null;
            }
            return super.decorate(loader, field);
        } catch (Exception ex) {
            if (ex instanceof WebDriverExtensionException) {
                throw (WebDriverExtensionException) ex; // re-throw it
            } else {
                throw new WebDriverExtensionException("Failed to decorate field " + field.getName() + " in class " + field.getDeclaringClass(), ex);
            }
        }

    }

    private boolean isDecoratableWebComponent(Field field) {
        if (!WebComponent.class.isAssignableFrom(field.getType())) {
            return false;
        }

        return true;
    }

    private boolean isDecoratableWebComponentList(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) {
            return false;
        }

        // Type erasure in Java isn't complete. Attempt to discover the generic
        // type of the list.
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }

        Type listType = ((ParameterizedType) genericType).getActualTypeArguments()[0];

        if (listType instanceof TypeVariable) {
            if (!WebComponent.class.isAssignableFrom(getListType(field, genericTypeArguments))) {
                return false;
            }
        } else if (listType instanceof ParameterizedType) {
            if (!WebComponent.class.isAssignableFrom((Class) ((ParameterizedType) listType).getRawType())) {
                return false;
            }
        } else {
            if (!WebComponent.class.isAssignableFrom((Class) listType)) {
                return false;
            }
        }

        if (field.getAnnotation(FindBy.class) == null
                && field.getAnnotation(FindBys.class) == null
                && field.getAnnotation(FindAll.class) == null) {
            return false;
        }

        return true;
    }


    private Object decorateWebComponent(ClassLoader loader, Field field, ParameterizedType genericTypeArguments) {
        ElementLocator locator = factory.createLocator(field);
        Class type = ReflectionUtils.getType(field, genericTypeArguments);
        final WebElement webElement = proxyForLocator(loader, locator);
        final WebComponent webComponent = webComponentFactory.create(type, webElement);
        PageFactory.initElements(new WebDriverExtensionFieldDecorator(webElement, driver, genericTypeArguments), webComponent);
        webComponent.delegateWebElement = WebDriverExtensionAnnotations.getDelagate(webComponent);
        return webComponent;
    }

    private Object decorateWebComponentList(final ClassLoader loader, final Field field, ParameterizedType genericTypeArguments) {
        ElementLocator locator = factory.createLocator(field);
        Class listType = ReflectionUtils.getListType(field, genericTypeArguments);
        List<WebElement> webElements = proxyForListLocator(loader, locator);
        return webComponentListFactory.create(listType, webElements, driver, genericTypeArguments);
    }

    public static Class getListType(Field field, ParameterizedType genericTypeArguments) {
        Type genericType = field.getGenericType();
        Type listType = ((ParameterizedType) genericType).getActualTypeArguments()[0];

        if (listType instanceof TypeVariable) {
            String genericTypeVariableName = ((TypeVariable) listType).getName();
            TypeVariable<?>[] classGenericTypeParameters = ((TypeVariable) listType).getGenericDeclaration().getTypeParameters();
            for (int i = 0; i < classGenericTypeParameters.length; i++) {
                if (classGenericTypeParameters[i].getName().equals(genericTypeVariableName)) {
                    return (Class) genericTypeArguments.getActualTypeArguments()[i];
                }
            }
            throw new WebDriverExtensionException("Could not find genericTypeVariableName = " + genericTypeVariableName + " in class");
        } if (listType instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) listType).getRawType();
        } else {
            return (Class) listType;
        }
    }
}
