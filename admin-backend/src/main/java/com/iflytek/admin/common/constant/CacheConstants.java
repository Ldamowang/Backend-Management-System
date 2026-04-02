package com.iflytek.admin.common.constant;

public final class CacheConstants {
    private CacheConstants() {}

    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    public static final String LOGIN_USER_PREFIX = "login:user:";
    public static final String ONLINE_USER_PREFIX = "online:user:";

    public static final String DICT_DATA_PREFIX = "dict:data:";
    public static final String DICT_TYPES_KEY = "dict:types";
    public static final String CONFIG_ALL_KEY = "config:all";
    public static final String CONFIG_PREFIX = "config:";
    public static final String MENU_USER_PREFIX = "menu:user:";
    public static final String PERM_USER_PREFIX = "perm:user:";
    public static final String IDEMPOTENT_PREFIX = "idempotent:";

    /** 字典缓存 TTL：24 小时（秒） */
    public static final long DICT_TTL = 86400;
    /** 系统配置缓存 TTL：24 小时（秒） */
    public static final long CONFIG_TTL = 86400;
    /** 菜单/权限缓存 TTL：2 小时（秒） */
    public static final long MENU_PERM_TTL = 7200;
}
