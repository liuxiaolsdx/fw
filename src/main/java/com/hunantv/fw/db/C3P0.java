package com.hunantv.fw.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;

import com.hunantv.fw.utils.FwLogger;
import com.hunantv.fw.utils.StringUtil;
import com.hunantv.fw.utils.SysConf;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3P0 {
    public static final FwLogger logger = new FwLogger(C3P0.class);
    private static C3P0 c3p0 = null;
    private Map<String, DataSource> dataSources = new HashMap<>();
    public static final String DEFUAT_DB_NAME = "write";

    private C3P0() {
        try {
            Map<String, Map<String, Object>> specialPros = initPros();
            for (Iterator<String> iter = specialPros.keySet().iterator(); iter.hasNext(); ) {
                String name = iter.next();
                printPros(name, specialPros.get(name));
                DataSource ds = new ComboPooledDataSource();
                BeanUtils.populate(ds, specialPros.get(name));
                this.dataSources.put(name, ds);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void printPros(String name, Map<String, Object> pros) {
        logger.debug("datasource name:" + name);
        pros.forEach((k, v) -> logger.debug(k + "=" + v));
    }

    private Map<String, Map<String, Object>> initPros() {
        try {
            Properties pros = new SysConf().read("c3p0.properties");

            Map<String, Object> sharePros = new HashMap<>();
            String[] names = StringUtil.split(((String) pros.get("c3p0.names")).trim(), ",");

            Map<String, Map<String, Object>> specialPros = new HashMap<>();
            for (String name : names) {
                specialPros.put(name, new HashMap<>());
            }

            for (Iterator iter = pros.keySet().iterator(); iter.hasNext(); ) {
                String key = (String) iter.next();
                if (key.startsWith("c3p0.") && !key.startsWith("c3p0.names")) {
                    for (String specialName : specialPros.keySet()) {
                        if (!key.startsWith("c3p0." + specialName)) {
                            sharePros.put(key.substring(5), pros.get(key));
                        } else {
                            // c3p0. 这个长度是5，最后一个 1是最后还有一个 . 的长度
                            int len = 5 + specialName.length() + 1;
                            specialPros.get(specialName).put(key.substring(len), pros.get(key));
                        }
                    }
                }
            }

            /**
             * 把常规属性放到特殊属性里面去
             */
            for (Map<String, Object> ps : specialPros.values()) {
                for (Iterator<String> iter = sharePros.keySet().iterator(); iter.hasNext(); ) {
                    String key = iter.next();
                    ps.put(key, sharePros.get(key));
                }
            }
            return specialPros;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static C3P0 instance() {
        if (c3p0 == null)
            c3p0 = new C3P0();
        return c3p0;
    }

    public DataSource getDataSource() {
        return this.getDataSource(this.DEFUAT_DB_NAME);
    }

    public DataSource getDataSource(String name) {
        return this.dataSources.get(name);
    }

    public static void main(String[] args) throws Exception {
        ComboPooledDataSource ds = (ComboPooledDataSource) new C3P0().getDataSource();
        System.out.println(ds.getMaxPoolSize());
    }
}
