package com.dianping.puma.biz.service.remote;

import com.dianping.puma.alarm.model.meta.EmailAlarmMeta;
import com.dianping.puma.alarm.model.meta.LogAlarmMeta;
import com.dianping.puma.alarm.model.meta.SmsAlarmMeta;
import com.dianping.puma.alarm.model.meta.WeChatAlarmMeta;
import com.dianping.puma.alarm.service.PumaClientAlarmMetaService;
import com.dianping.puma.biz.convert.Converter;
import com.dianping.puma.biz.dao.ClientAlarmMetaDao;
import com.dianping.puma.biz.entity.ClientAlarmMetaEntity;

import java.util.Map;

/**
 * Created by xiaotian.li on 16/3/22.
 * Email: lixiaotian07@gmail.com
 */
public class RemoteClientAlarmMetaService implements PumaClientAlarmMetaService {

    private Converter converter;

    private ClientAlarmMetaDao clientAlarmMetaDao;

    @Override
    public EmailAlarmMeta findEmail(String clientName) {
        ClientAlarmMetaEntity entity = clientAlarmMetaDao.find(clientName);
        return converter.convert(entity, EmailAlarmMeta.class);
    }

    @Override
    public WeChatAlarmMeta findWeChat(String clientName) {
        ClientAlarmMetaEntity entity = clientAlarmMetaDao.find(clientName);
        return converter.convert(entity, WeChatAlarmMeta.class);
    }

    @Override
    public SmsAlarmMeta findSms(String clientName) {
        ClientAlarmMetaEntity entity = clientAlarmMetaDao.find(clientName);
        return converter.convert(entity, SmsAlarmMeta.class);
    }

    @Override
    public LogAlarmMeta findLog(String clientName) {
        ClientAlarmMetaEntity entity = clientAlarmMetaDao.find(clientName);
        return converter.convert(entity, LogAlarmMeta.class);
    }

    @Override
    public Map<String, EmailAlarmMeta> findEmailAll() {
        return null;
    }

    @Override
    public Map<String, WeChatAlarmMeta> findWeChatAll() {
        return null;
    }

    @Override
    public Map<String, SmsAlarmMeta> findSmsAll() {
        return null;
    }

    @Override
    public Map<String, LogAlarmMeta> findLogAll() {
        return null;
    }

    @Override
    public void replaceEmail(String clientName, EmailAlarmMeta meta) {

    }

    @Override
    public void replaceWeChat(String clientName, WeChatAlarmMeta meta) {

    }

    @Override
    public void replaceSms(String clientName, SmsAlarmMeta meta) {

    }

    @Override
    public void replaceLog(String clientName, LogAlarmMeta meta) {

    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public void setClientAlarmMetaDao(ClientAlarmMetaDao clientAlarmMetaDao) {
        this.clientAlarmMetaDao = clientAlarmMetaDao;
    }
}