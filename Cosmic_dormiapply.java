import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.gpt.IGPTAction;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 宿舍申请单动态表单插件
 */
public class Cosmic_dormiapply implements IGPTAction {
    @Override
    public Map<String, String> invokeAction(String action, Map<String, String> params) {
        Map<String, String> resultMap = new HashMap<>();
        if ("GET_JSON_STRING".equalsIgnoreCase(action)) {
            // 获取Json字符串
            String jsonResult = params.get("jsonResult").replaceAll("\\s*|\r|\n|\t", "");
            JSONObject resultJsonObject = null;
            try {
                resultJsonObject = JSON.parseObject(jsonResult);
            } catch (Exception ee) {
                jsonResult = jsonResult.substring(jsonResult.indexOf("\"applicant\"") - 1,
                        jsonResult.indexOf("}") + 1);
                resultJsonObject = JSON.parseObject(jsonResult);
            }
            System.out.println("resultJsonObject: " + resultJsonObject);

            // 随机一个单据编号
            StringBuilder sb1 = new StringBuilder();
            for (int i = 1; i <= 10; ++i) {
                int ascii = 48 + (int) (Math.random() * 9);
                char c = (char) ascii;
                sb1.append(c);
            }

            // 提炼出json信息
            String applicantId = resultJsonObject.getString("applicant");
            String apartmentId = resultJsonObject.getString("apartment");
            String roomId = resultJsonObject.getString("room");

            // 筛出该申请人的基础资料，方便后续set到宿舍申请单据中
            QFilter qFilter = new QFilter("id", QCP.equals, applicantId);
            DynamicObject student = BusinessDataServiceHelper.loadSingle("tz94_student", new QFilter[] { qFilter });

            // 获取该学生的对应信息
            String studentname = student.getString("studentname");
            String studentacademy = student.getString("studentacademy");
            String studentmajor = student.getString("studentmajor");
            String studenttelephone = student.getString("studenttelephone");
            String studentphoto = student.getString("studentphoto");
            String studentsex = student.getString("studentsex");
            String befapart = student.getString("befapart"); // 原宿舍楼ID
            String befroom = student.getString("befroom"); // 原房间ID

            // 查询申请的宿舍楼信息
            QFilter apartmentFilter = new QFilter("id", QCP.equals, apartmentId);
            DynamicObject apartment = BusinessDataServiceHelper.loadSingle("tz94_apartment",
                    new QFilter[] { apartmentFilter });

            // 查询申请的房间信息
            QFilter roomFilter = new QFilter("id", QCP.equals, roomId);
            DynamicObject room = BusinessDataServiceHelper.loadSingle("tz94_room", new QFilter[] { roomFilter });

            // 获取当前日期
            LocalDate today = LocalDate.now();
            Date todayDate = Date.valueOf(today);

            // new 一个 DynamicObject 表单对象
            DynamicObject dynamicObject = BusinessDataServiceHelper.newDynamicObject("tz94_dormiapply");

            // 设置对应属性
            dynamicObject.set("billno", sb1.toString());
            dynamicObject.set("tz94_applicant", student); // 申请人基础资料
            dynamicObject.set("tz94_studentname", studentname);
            dynamicObject.set("tz94_studentacademy", studentacademy);
            dynamicObject.set("tz94_studentmajor", studentmajor);
            dynamicObject.set("tz94_studenttelephone", studenttelephone);
            dynamicObject.set("tz94_studentphoto", studentphoto);
            dynamicObject.set("tz94_studentsex", studentsex);
            dynamicObject.set("tz94_befapart", befapart);
            dynamicObject.set("tz94_befroom", befroom);
            dynamicObject.set("tz94_apartment", apartment); // 申请宿舍楼
            dynamicObject.set("tz94_room", room); // 申请房间
            dynamicObject.set("createtime", todayDate);

            SaveServiceHelper.saveOperate("tz94_dormiapply", new DynamicObject[] { dynamicObject }, null);
            Long pkId = (Long) dynamicObject.getPkValue();

            // 拼接URL字符串
            String targetForm = "bizAction://currentPage?gaiShow=1&selectedProcessNumber=processNumber&gaiAction=showBillForm&gaiParams={\"appId\":\"tz94_dormitory\",\"billFormId\":\"tz94_dorm_application\",\"billPkId\":\""
                    + pkId + "\"}&title=宿舍申请单&iconType=bill&method=bizAction";
            resultMap.put("formUrl", targetForm);
        }
        return resultMap;
    }
}