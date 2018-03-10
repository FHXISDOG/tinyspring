package controller;

import annotion.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@controller("/test")
public class TestController {

    @Qualifier("testService")
    TestService testService;

    @RequestMapping("/get")
    public String hello(HttpServletRequest request, HttpServletResponse response,String param) {
        testService.get();
        return "success";
    }

}
