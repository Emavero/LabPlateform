package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.VpnProfileJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataVpnProfileRepository;
import com.labplatform.application.port.out.VpnProfileRepositoryPort;
import com.labplatform.domain.vpn.VpnProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VpnProfilePersistenceAdapter implements VpnProfileRepositoryPort {

    private final SpringDataVpnProfileRepository repository;

    public VpnProfilePersistenceAdapter(SpringDataVpnProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<VpnProfile> findByUserId(Long userId) {
        return repository.findById(userId)
                .map(e -> VpnProfile.restore(e.getUserId(), e.getCommonName(), e.getIssuedAt()));
    }

    @Override
    public VpnProfile save(VpnProfile profile) {
        VpnProfileJpaEntity saved = repository.save(
                new VpnProfileJpaEntity(profile.getUserId(), profile.getCommonName(), profile.getIssuedAt()));
        return VpnProfile.restore(saved.getUserId(), saved.getCommonName(), saved.getIssuedAt());
    }
}
