package com.abe.controller;


import com.abe.bean.abeuser;
import com.abe.post.AbefileSQLpost;
import com.abe.post.AberequestSQLpost;
import com.abe.post.AbeuserSQLpost;
import com.abe.tool.CommonResponse;
import com.abe.tool.Constant;
import com.abe.CPABE;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.json.JSONException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;


@RestController
public class ABEController {
    AbeuserSQLpost abeuserSQLpost=new AbeuserSQLpost();
    AbefileSQLpost abefileSQLpost=new AbefileSQLpost();
    AberequestSQLpost aberequestSQLpost= new AberequestSQLpost();
    private static String data;
    @PostMapping("/initkey")
    public CommonResponse initKey(@RequestParam("userid") Long id, @RequestParam("securitylabel") String securityLabel){

        String pairingParametersFileName =  "security_params/security_level_" + securityLabel + "bits.properties";
        String dirproperty = System.getProperty("user.dir");
        String filedata=dirproperty+ Constant.abeFile;
        File file = new File(filedata);
        if ( !file.exists())   file.mkdir();

        String File=filedata+"/"+id;
        file = new File(File);
        if ( !file.exists())   file.mkdir();
        String pkFileName=File+"/security_level_"+securityLabel +"publickey";
        String mskFileName=File+"/security_level_"+securityLabel +"masterkey";
        CPABE.setup(pairingParametersFileName, pkFileName, mskFileName);
        String initKey = abeuserSQLpost.abeuserInitKey(id, pkFileName, mskFileName);
        CommonResponse<Object> commonResponse = new CommonResponse<>(200, initKey);
        return commonResponse;
    }
    @PostMapping("/encryptFile")
    public CommonResponse encryptFile(@RequestParam("userid") Long id,@RequestParam("file") MultipartFile file, @RequestParam("policyfile") MultipartFile policyFile) throws IOException, GeneralSecurityException, JSONException {
        abeuser abeuser = abeuserSQLpost.getKey(id);
        if (abeuser == null) {
            return new CommonResponse<>(300, "未初始化");
        }
        String fileid = UUID.randomUUID().toString();
        String dirproperty = System.getProperty("user.dir");
        String ctFileName = dirproperty + Constant.abeFile + "/" + id + "/ct_" + fileid;
        String policyFileName = dirproperty + Constant.abeFile + "/" + id + "/policy_" + fileid;
        saveFile(policyFile,policyFileName);
        FileInputStream fileInputStream = (FileInputStream) file.getInputStream();
        byte[] b = new byte[(int) file.getSize()];  //定义文件大小的字节数据
        fileInputStream.read(b);//将文件数据存储在b数组
        data = new String(b, StandardCharsets.UTF_8); //将字节数据转换为UTF-8编码的字符串
        String pkFileName = abeuser.getPublickey();
        CPABE.kemEncrypt(data, policyFileName, pkFileName, ctFileName);
        String addfile = abefileSQLpost.addfile(id, fileid, ctFileName, policyFileName);
        return new CommonResponse<>(200, addfile);

    }
    private static void saveFile(MultipartFile file,String filename){
        FileOutputStream fileOutputStream = null;
        InputStream inputStream = null;
        try {
            if ((!file.isEmpty())) {
                //保存

                fileOutputStream = new FileOutputStream(filename);
                inputStream = file.getInputStream();
                IOUtils.copy(inputStream, fileOutputStream);
                fileOutputStream.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}