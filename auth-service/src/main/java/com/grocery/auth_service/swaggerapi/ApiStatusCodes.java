package com.grocery.auth_service.swaggerapi;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ApiStatusCodes {

    public static final String OK="200";
    public static final String CREATED="201";
    public static final String NO_CONTENT="204";
    public static final String BAD_REQUEST="400";
    public static final String UNAUTHORIZED="401";
    public static final String FORBIDDEN="403";
    public static final String NOT_FOUND="404";
    public static final String METHOD_NOT_ALLOWED="405";
    public static final String CONFLICT="409";
    public static final String INTERNAL_SERVER_ERROR="500";
    public static final String BAD_GATEWAY="502";
    public static final String SERVICE_UNAVAILABLE="503";

}
