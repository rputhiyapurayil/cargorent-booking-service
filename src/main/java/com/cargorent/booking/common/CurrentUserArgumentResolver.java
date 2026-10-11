package com.cargorent.booking.common;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Lets controllers declare a {@link CurrentUser} parameter that is filled from gateway headers. */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String id = request.getHeader("X-User-Id");
        Long userId = null;
        if (id != null && !id.isBlank()) {
            try {
                userId = Long.valueOf(id);
            } catch (NumberFormatException ignored) {
                // treated as anonymous
            }
        }
        return new CurrentUser(userId, request.getHeader("X-User-Role"),
                decode(request.getHeader("X-User-Name")), decode(request.getHeader("X-User-Email")));
    }

    private static String decode(String value) {
        return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
