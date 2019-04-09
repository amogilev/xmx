// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.Advice;
import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.Argument;
import com.gilecode.xmx.aop.RetVal;
import com.gilecode.xmx.boot.XmxProxy;
import org.springframework.util.PropertyPlaceholderHelper;

public class SpringResolvedPlaceholderInterceptor {

//    private static final IAdviceLogger logger = LoggerFactory.getLogger(SpringResolvedPlaceholderInterceptor.class);

    /**
     * This advice is supposed to intercept
     * {@link PropertyPlaceholderHelper#replacePlaceholders(String, PropertyPlaceholderHelper.PlaceholderResolver)}
     * to track resolved placeholders and properties.
     */
    @Advice(AdviceKind.AFTER_RETURN)
    public void intercept(@Argument(0) String value, @RetVal String resolved) {
        if (resolved != null && value != null && !resolved.equals(value)) {
            XmxProxy.fireAdviceEvent(AdvicesConstants.PLUGIN_SPRING, "ResolvedPlaceholder", value, resolved);
        }
    }
}
