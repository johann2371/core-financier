package com.corefi.repository;

import com.corefi.entity.Parametrage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "DB_URL=jdbc:h2:mem:testdb", "DB_USERNAME=sa", "DB_PASSWORD=", 
    "MAIL_USERNAME=test", "MAIL_PASSWORD=test", "MINDEE_API_KEY=test", 
    "JWT_SECRET=this_is_a_very_long_secret_key_for_testing_purposes_only_32_chars", "JWT_EXPIRATION=3600"
})
class ParametrageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParametrageRepository parametrageRepository;

    private Parametrage parametrage;

    @BeforeEach
    void setUp() {
        parametrage = new Parametrage();
        parametrage.setCle("APP_NAME");
        parametrage.setValeur("CoreFi");
        parametrage.setDescription("Desc");
        entityManager.persist(parametrage);
        entityManager.flush();
    }

    @Test
    @DisplayName("Doit trouver un paramètre par sa clé")
    void findByCle_existingCle_returnsParametrage() {
        // when
        Optional<Parametrage> found = parametrageRepository.findByCle("APP_NAME");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getValeur()).isEqualTo("CoreFi");
    }

    @Test
    @DisplayName("Ne doit pas trouver de paramètre pour une clé inexistante")
    void findByCle_nonExistentCle_returnsEmpty() {
        // when
        Optional<Parametrage> found = parametrageRepository.findByCle("UNKNOWN");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Doit sauvegarder un paramètre")
    void save_validParam_returnsSavedParam() {
        // given
        Parametrage newParam = new Parametrage();
        newParam.setCle("NEW_KEY");
        newParam.setValeur("NewValue");

        // when
        Parametrage saved = parametrageRepository.save(newParam);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCle()).isEqualTo("NEW_KEY");
    }

    @Test
    @DisplayName("Doit retourner Optional vide pour une clé nulle")
    void findByCle_nullCle_returnsEmpty() {
        // when
        Optional<Parametrage> found = parametrageRepository.findByCle(null);

        // then
        assertThat(found).isEmpty();
    }
}
