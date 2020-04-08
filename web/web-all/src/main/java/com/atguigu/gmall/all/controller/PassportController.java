package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author jiahao
 * @create 2020-03-25 16:59
 */
@Controller
public class PassportController {
    @GetMapping("login.html")
    //http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
    public String login(HttpServletRequest request) {
        //如果登陆成功  返回originUrl=http://www.gmall.com路径
        String originUrl = request.getParameter("originUrl");
        //保存起来
        request.setAttribute("originUrl",originUrl);
        //返回登录页面
        return "login";
    }
}
