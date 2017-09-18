package com.lyl57.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by lyl57 on 2017/3/13.
 */
@RestController
@RequestMapping("/hello1")
public class HelloController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/")
    public String test1() {
        log.info("test");
        return "mng:Hello World";
    }


}
