package com.labplatform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Le montage Compute Engine est nécessaire dès que l'un des deux usages le
 * demande : la cible partagée de /api/machine, ou les cibles du catalogue sous
 * le mode d'hyperviseur « gcp ». Les deux parlent au même projet et partagent
 * le même client ; la condition est donc écrite une fois, ici, plutôt que
 * recopiée sur chaque bean.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression(
        "'${app.machine.provider:simulated}' == 'gcp' or '${app.hypervisor.mode:simulated}' == 'gcp'")
public @interface ConditionalOnGcp {
}
