package io.github.chrisruffalo.pintle.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Name;

import java.util.List;
import java.util.Set;

public class NameUtilTest {

    @Test
    public void domains() {
        Set<String> domains = NameUtil.domains("vector.images.google.com");
        Assertions.assertEquals(4, domains.size());
        Assertions.assertTrue(domains.contains("vector.images.google.com."));

        domains = NameUtil.domains("really.long.infeasible.domain.name.that.does.not.exist.com");
        Assertions.assertEquals(10, domains.size());

        domains = NameUtil.domains("really.long.infeasible.domain.name.that.does.not.exist.com.");
        Assertions.assertEquals(10, domains.size());

        domains = NameUtil.domains(".");
        Assertions.assertEquals(1, domains.size());

        domains = NameUtil.domains("");
        Assertions.assertEquals(0, domains.size());

        domains = NameUtil.domains((String)null);
        Assertions.assertEquals(0, domains.size());

        domains = NameUtil.domains(Name.empty);
        Assertions.assertEquals(0, domains.size());
    }

    @Test
    public void intersects() {
        Assertions.assertTrue(NameUtil.intersects("att.com", "att.com", "att.net"));
        Assertions.assertTrue(NameUtil.intersects("mail.att.com", "att.com", "att.net"));
        Assertions.assertTrue(NameUtil.intersects("att.net", "att.com", "att.net"));
        Assertions.assertTrue(NameUtil.intersects("login.att.net", "att.com", "att.net"));
    }

}
