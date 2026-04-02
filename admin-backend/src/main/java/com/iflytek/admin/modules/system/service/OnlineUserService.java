package com.iflytek.admin.modules.system.service;

import java.util.List;
import java.util.Map;

public interface OnlineUserService {
    List<Map<String, Object>> listOnlineUsers();
    void kickOut(String token);
}
