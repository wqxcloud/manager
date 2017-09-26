package com.suny.association.controller.backed;

import com.suny.association.enums.BaseEnum;
import com.suny.association.pojo.po.Account;
import com.suny.association.pojo.po.Member;
import com.suny.association.service.interfaces.IAccountService;
import com.suny.association.service.interfaces.ILoginService;
import com.suny.association.utils.JsonResult;
import com.suny.association.utils.TokenProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comments:   基础公共Controller
 * Author:   孙建荣
 * Create Date: 2017/03/05 11:05
 */
@RequestMapping("/backend")
@Controller
public class BackedLoginController {

    private static Logger logger = LoggerFactory.getLogger(BackedLoginController.class);
    private static final String TICKET = "ticket";

    private final IAccountService accountService;


    private final ILoginService loginService;

    private static final String TOKEN = "token";

    @Autowired
    public BackedLoginController(IAccountService accountService, ILoginService loginService) {
        this.accountService = accountService;
        this.loginService = loginService;
    }



    /**
     * 登录页面
     */
    @RequestMapping(value = {"/","/login.html"}, method = RequestMethod.GET)
    public String backendLogin(HttpServletRequest request) {
        String token = TokenProcessor.getInstance().makeToken();
        request.getSession().setAttribute(TOKEN, token);
        logger.info("产生的令牌值是 {}", token);
        return "backend/login";
    }


    /**
     * 全局错误页面
     */
    @RequestMapping(value = "/error.html", method = RequestMethod.GET)
    public ModelAndView errorPage() {
        return new ModelAndView("error");
    }


    /**
     * 提交登录信息操作
     *
     * @param username 登录用户名
     * @param password 登录密码
     * @param formCode 表单过来的验证码
     * @param request  request请求
     * @return 验证的json结果
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(value = "/login.action", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult loginAction(@RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  @RequestParam("formCode") String formCode,
                                  @RequestParam(value = "rememberMe", defaultValue = "true") Boolean rememberMe,
                                  @RequestParam("token") String token,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        //    1.  首先验证表单提交的token跟session里面的token是否相等，相等就说明不是重复提交
        if (!isRepeatSubmit(token, request)) {
            //   1.1   把session里面的token标记先移除
            request.getSession().removeAttribute(TOKEN);
            //   1.2    获取session中保存的本次服务器下发的验证码
//            String sessionCode = (String) request.getSession().getAttribute("code");
            //   1.3    匹配session里面的验证码跟表单上的验证码是否相等，这里为了开发方便就先关闭
//            if (!ValidActionUtil.matchCode(formCode, sessionCode)) {
//                return JsonResult.failResult(BaseEnum.VALIDATE_CODE_ERROR);
//            }
            //   1.4   获取登录的结果,也就是带有ticket则表示登录成功了
            AtomicReference<Map<String, Object>> loginResult = loginService.login(username, password);
            if (loginResult.get().containsKey(TICKET)) {
                //   1.4.1    把获取到的ticket放到Cookie中去
                String ticket = (String) loginResult.get().get(TICKET);
                Cookie cookie = new Cookie(TICKET, ticket);
                cookie.setPath("/");
                if (rememberMe) {
                    cookie.setMaxAge(3600 * 24 * 5);
                }
                response.addCookie(cookie);
                //    1.4.4   把一些进入主页面需要的数据先放进去
                saveUser(response, username);
                logger.warn("登录成功了,给前端发送通知");
                return JsonResult.successResult(BaseEnum.LOGIN_SYSTEM);
            }
            //   1.5   没有返回ticket就是登录失败了,可能是由于面膜错误,账号错误，账号密码不匹配，参数为空等等
            return JsonResult.failResult(BaseEnum.LOGIN_FAILURE);
        }
        //   2.  重复提交表单的业务逻辑处理
        logger.warn("重复提交表单");
        return JsonResult.failResult(BaseEnum.REPEAT_SUBMIT);
    }


    /**
     * 验证token是否相同，防止CSRF或者重复提交表单
     *
     * @param token   令牌值
     * @param request request值
     * @return 比较的结果
     */
    private boolean isRepeatSubmit(String token, HttpServletRequest request) {
        // 如果token是空的则说明重复提交了表单
        if ("".equals(token) || token == null) {
            return true;
        }
        String sessionToken = (String) request.getSession().getAttribute(TOKEN);
        if (sessionToken == null) {
            return true;
        }
        return sessionToken.equals(token) ? false : false;
    }


    /**
     * 验证成功后报存用户的登录信息
     *
     * @param response  response请求
     * @param username 登录的用户名
     */
    private void saveUser(HttpServletResponse response, String username) {
        Member member = accountService.selectByName(username).getAccountMember();
        Account account = accountService.selectByName(username);
        Cookie nameCookie = new Cookie("account", account.getAccountName());
        Cookie accountCookie = new Cookie("member", member.getMemberName());
        response.addCookie(nameCookie);
        response.addCookie(accountCookie);
//        request.getSession().setAttribute("member", member);
//        request.getSession().setAttribute("account", account);
    }


    /**
     * 登录成功后的操作
     *
     * @return 管理员中心
     */
    @RequestMapping(value = {"/userCenter.html"}, method = RequestMethod.GET)
    public ModelAndView userCenter() {
        return new ModelAndView("backend/userCenter");
    }



}









