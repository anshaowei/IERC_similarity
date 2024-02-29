package net.csibio.metaphoenix.core.config;

import jakarta.annotation.PostConstruct;
import net.csibio.metaphoenix.client.utils.RepositoryUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("vmProperties")
public class VMProperties {
    @Value("E:\\MS_data\\MetaPhoenix\\repository")
    private String repository;

    @PostConstruct
    public void init() {
        RepositoryUtil.repository = repository;
    }

    public String getRepository() {
        if (StringUtils.isEmpty(repository)) {
            return "/nas/data";
        }
        return repository;
    }

}
