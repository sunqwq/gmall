package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试JwtTest 工具类
 */
public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\IO\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\IO\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    //在公钥路径下生成公钥 , 在私钥路径下生成私钥 , secret 指加盐,越复杂越好
    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "sdada234");
    }

    //根据指定路径读取公钥私钥
    //@BeforeEach 指在所有test方法执行前 先执行
    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    //生成JWT
    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    //解析JWT
    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTgyODg3MjN9.RflbJmtIVGR3veqnhpPm0kwp4lczpil9WnMkU393zdwsy_mEAQ25-WbKMu1uj53m5DzKB5fbfyXj04gJidlBQbBK5CFCNyEs9SUAsBg1zXari6ILTM8vJu0TkrkxsfFb1vkC06j2gl0kqX6G4kJ1q7MgLUTyp1mUToaV32rCqpJY_B08icQ7JVxlJZQu7dZxqauyFERwkwPQh5XIjbPpO4IlVrZy6FY-kol8TXYpzdGvqJCwMUvq_BuexHjUAXPzfikNeaLQ7s2m-WcOuiIo-atrhlr7RdPGeou6GjUMhT5MufR05fztOcGIPbaOs1BAn_DqHUsxE3jQ4QA3lZSAvg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}