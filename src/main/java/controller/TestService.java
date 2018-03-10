package controller;

import annotion.Service;

@Service("testService")
public class TestService {

    public void get() {
        System.out.println("hello service");
    }
}
