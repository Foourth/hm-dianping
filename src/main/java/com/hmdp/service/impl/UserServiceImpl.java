package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class);
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sedCode(String phone, HttpSession session) {
        // 1 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号输入错误");
        }
        // 2 生成验证码
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute("code", code);
        log.debug("返回验证码成功：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号输入错误");
        }
        //2. 校验验证码
        Object code = session.getAttribute("code");
        String formCode = loginForm.getCode();
        //3. 不一致，报错
        if (code == null || !code.toString().equals(formCode)) {
            return Result.fail("验证码错误");
        }
        //4.一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //5. 判断用户是否存在
        if (user == null) {
            //6. 不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        //7.保存用户信息到session
        session.setAttribute("user", user);
        return Result.ok();
    }



    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }
}
