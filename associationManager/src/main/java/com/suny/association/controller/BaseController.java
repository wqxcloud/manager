package com.suny.association.controller;

import com.suny.association.enums.ErrorCode;
import com.suny.association.exception.BusinessException;
import com.suny.association.pojo.po.Account;
import com.suny.association.pojo.po.Member;
import com.suny.association.service.interfaces.IAccountService;
import com.suny.association.service.interfaces.IMemberService;
import com.suny.association.utils.DecriptUtils;
import com.suny.association.utils.JSONResponseUtil;
import com.suny.association.utils.JSONUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Comments:   基础公共Controller
 * Author:   孙建荣
 * Create Date: 2017/03/05 11:05
 */
@Controller
public class BaseController {
    
    @Autowired
    private IAccountService accountService;
    
    @Autowired
    private IMemberService memberService;
    
  
    
    
    /**
     * 直接输入根目录的话就是直接跳转到登陆页面
     * @return  登陆页面
     */
    @RequestMapping("/")
    public String indexPage(){
        return "/login";
    }
    
    /**
     * 进入到登陆页面
     *
     * @param request 请求参数
     * @return 登陆的页面
     * @throws Exception
     */
    @RequestMapping("/login.html")
    public String getIndex(HttpServletRequest request) throws Exception {
        return "redirect:/";
    }
    
    
    /**
     * 登陆成功后的跳转操作
     * @return     管理页面
     * @throws Exception
     */
    @RequestMapping("/admin-manager.html")
    public ModelAndView adminManager() throws Exception {
        ModelAndView mav = new ModelAndView("admin-manager");
        return mav;
    }
    
    
    /**
     * 强行访问没有权限的页面处理
     * @return   错误页面
     */
    @RequestMapping(value = "/jumpError.html")
    public String jumpErrorPage(){
        return "error";
    }
    
    
    
    
    /**
     *   验证用户输入的情况
     * @param username   登陆的用户名
     * @param password    登陆的用户密码
     * @return
     */
    @RequestMapping(value = "/checkLogin.json", method = RequestMethod.POST)
    @ResponseBody
    public String checkLogin(String username, String password,HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            UsernamePasswordToken token = new UsernamePasswordToken(username, DecriptUtils.encryptToMD5(password));
            Subject currentUser = SecurityUtils.getSubject();
            if (!currentUser.isAuthenticated()) {
                //使用shiro来验证
                token.setRememberMe(true);
                currentUser.login(token);//验证角色和权限
                //通过账号表里面的memberId查询到一个成员的信息
                Account account = accountService.selectByUserName(username);
                Member member = memberService.queryById(account.getAccountMemberId());
                request.getSession().setAttribute("member",member);
            }
            
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.LOGIN_VERIFY_FAILURE);
        }
        
        result.put("success", true);
        return JSONUtils.toJson(result);
    }
    
    /**
     * 退出登录
     */
    @RequestMapping(value = "/logout.do", method = RequestMethod.GET)
    @ResponseBody
    public JSONResponseUtil logout() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.logout();
        return JSONResponseUtil.success(result);
    }
    
    
}









