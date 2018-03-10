package servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotion.*;
import controller.*;


@WebServlet(urlPatterns = "/")
public class DisPatcherServlet extends HttpServlet {

    //用来存文件
    private List<String> classNames = new ArrayList<String>();
    //用来存实例
    private Map<String, Object> instanceMap = new HashMap<String, Object>();
    //方法和路径存放的url
    private Map<String, Method> methodMap = new HashMap<String, Method>();

    @Override
    public void init() throws ServletException {
        /**
         * servlet初始化
         */
        super.init();
        /**
         * 找到bean
         */
        scanBase("controller");



        try {
            // 生成并注册bean
            filterAndInstance();
            //注入bean
            springDI();
            //处理请求映射
            mvc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mvc() {

        if (instanceMap.size() == 0) {
            return;
        } else {
            for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
                if (entry.getValue().getClass().isAnnotationPresent(controller.class)) {
                   String ctrlUrl =  ((controller) entry.getValue().getClass().getAnnotation(controller.class)).value();
                   Method[] methods=entry.getValue().getClass().getMethods();
                    for (Method m : methods) {
                        if (m.isAnnotationPresent(RequestMapping.class)) {
                            String reqUrl =  (m.getAnnotation(RequestMapping.class)).value();
                            String dispatchUrl =    ctrlUrl+reqUrl;
                            methodMap.put(dispatchUrl, m);
                        }
                    }
                }
            }
        }
    }

    //注入bean
    private void springDI() throws IllegalAccessException {
        if (instanceMap.size() == 0) {
            return;
        } else {
            for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
                Field[] fileds = entry.getValue().getClass().getDeclaredFields();
                for (Field field : fileds) {
                    if (field.isAnnotationPresent(Qualifier.class)) {
                        String key = ((Qualifier) field.getAnnotation(Qualifier.class)).value();
                        field.setAccessible(true);
                        field.set(entry.getValue(), instanceMap.get(key));
                        field.setAccessible(false);
                    }
                }
            }
        }
    }

    /**
     * 生成并且注册bean
     */
    private void filterAndInstance() throws Exception {
        if (classNames.size() == 0) {
            return;
        } else {
            for (String className : classNames) {
                Class clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(controller.class)) {
                    //获取实例
                    Object instance = clazz.newInstance();
                    //获取注解的value
                    String key = ((controller) clazz.getAnnotation(controller.class)).value();
                    //交付给ioc容器管理起来
                    instanceMap.put(key, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    //获取实例
                    Object instance = clazz.newInstance();
                    //获取注解的value
                    String key = ((Service) clazz.getAnnotation(Service.class)).value();
                    //交付给ioc容器管理起来
                    instanceMap.put(key, instance);
                } else {
                    continue;
                }
            }
        }
    }

    /**
     * 扫描所有文件
     *
     * @param java 类所在的包
     */
    private void scanBase(String java) {
        URL url = this.getClass().getClassLoader().getResource("/" + replacePath(java));
        String path = url.getFile();
        File file = new File(path);
        String[] strFiles = file.list();
        for (String str : strFiles) {
            File eacheFile = new File(path + str);
            if (eacheFile.isDirectory()) {
                scanBase(java + "." + eacheFile);
            } else {
                System.out.println("class name is :" + eacheFile.getName());
                classNames.add(java + "." + eacheFile.getName().replaceAll(".class",""));
            }
        }
    }


    private String replacePath(String path) {
        return path.replace("\\.", "/");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //localhost:8080/springioc/fish/get
        String url = request.getRequestURI();
        Method method = methodMap.get(url);
        String controllerName = url.split("/")[1];
        TestController controller = null;

            controller = (TestController) instanceMap.get("/"+controllerName);

        try {
            method.invoke(controller, new Object[]{request, response, null});
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }
}
