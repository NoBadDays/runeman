package nobaddays.runman.web.rest;

import nobaddays.runman.RuneManApp;
import nobaddays.runman.domain.Measure;
import nobaddays.runman.repository.MeasureRepository;
import nobaddays.runman.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static nobaddays.runman.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link MeasureResource} REST controller.
 */
@SpringBootTest(classes = RuneManApp.class)
public class MeasureResourceIT {

    private static final String DEFAULT_UNIT = "AAAAAAAAAA";
    private static final String UPDATED_UNIT = "BBBBBBBBBB";

    @Autowired
    private MeasureRepository measureRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restMeasureMockMvc;

    private Measure measure;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MeasureResource measureResource = new MeasureResource(measureRepository);
        this.restMeasureMockMvc = MockMvcBuilders.standaloneSetup(measureResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Measure createEntity(EntityManager em) {
        Measure measure = new Measure()
            .unit(DEFAULT_UNIT);
        return measure;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Measure createUpdatedEntity(EntityManager em) {
        Measure measure = new Measure()
            .unit(UPDATED_UNIT);
        return measure;
    }

    @BeforeEach
    public void initTest() {
        measure = createEntity(em);
    }

    @Test
    @Transactional
    public void createMeasure() throws Exception {
        int databaseSizeBeforeCreate = measureRepository.findAll().size();

        // Create the Measure
        restMeasureMockMvc.perform(post("/api/measures")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(measure)))
            .andExpect(status().isCreated());

        // Validate the Measure in the database
        List<Measure> measureList = measureRepository.findAll();
        assertThat(measureList).hasSize(databaseSizeBeforeCreate + 1);
        Measure testMeasure = measureList.get(measureList.size() - 1);
        assertThat(testMeasure.getUnit()).isEqualTo(DEFAULT_UNIT);
    }

    @Test
    @Transactional
    public void createMeasureWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = measureRepository.findAll().size();

        // Create the Measure with an existing ID
        measure.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMeasureMockMvc.perform(post("/api/measures")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(measure)))
            .andExpect(status().isBadRequest());

        // Validate the Measure in the database
        List<Measure> measureList = measureRepository.findAll();
        assertThat(measureList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkUnitIsRequired() throws Exception {
        int databaseSizeBeforeTest = measureRepository.findAll().size();
        // set the field null
        measure.setUnit(null);

        // Create the Measure, which fails.

        restMeasureMockMvc.perform(post("/api/measures")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(measure)))
            .andExpect(status().isBadRequest());

        List<Measure> measureList = measureRepository.findAll();
        assertThat(measureList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMeasures() throws Exception {
        // Initialize the database
        measureRepository.saveAndFlush(measure);

        // Get all the measureList
        restMeasureMockMvc.perform(get("/api/measures?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(measure.getId().intValue())))
            .andExpect(jsonPath("$.[*].unit").value(hasItem(DEFAULT_UNIT.toString())));
    }
    
    @Test
    @Transactional
    public void getMeasure() throws Exception {
        // Initialize the database
        measureRepository.saveAndFlush(measure);

        // Get the measure
        restMeasureMockMvc.perform(get("/api/measures/{id}", measure.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(measure.getId().intValue()))
            .andExpect(jsonPath("$.unit").value(DEFAULT_UNIT.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingMeasure() throws Exception {
        // Get the measure
        restMeasureMockMvc.perform(get("/api/measures/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMeasure() throws Exception {
        // Initialize the database
        measureRepository.saveAndFlush(measure);

        int databaseSizeBeforeUpdate = measureRepository.findAll().size();

        // Update the measure
        Measure updatedMeasure = measureRepository.findById(measure.getId()).get();
        // Disconnect from session so that the updates on updatedMeasure are not directly saved in db
        em.detach(updatedMeasure);
        updatedMeasure
            .unit(UPDATED_UNIT);

        restMeasureMockMvc.perform(put("/api/measures")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMeasure)))
            .andExpect(status().isOk());

        // Validate the Measure in the database
        List<Measure> measureList = measureRepository.findAll();
        assertThat(measureList).hasSize(databaseSizeBeforeUpdate);
        Measure testMeasure = measureList.get(measureList.size() - 1);
        assertThat(testMeasure.getUnit()).isEqualTo(UPDATED_UNIT);
    }

    @Test
    @Transactional
    public void updateNonExistingMeasure() throws Exception {
        int databaseSizeBeforeUpdate = measureRepository.findAll().size();

        // Create the Measure

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeasureMockMvc.perform(put("/api/measures")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(measure)))
            .andExpect(status().isBadRequest());

        // Validate the Measure in the database
        List<Measure> measureList = measureRepository.findAll();
        assertThat(measureList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMeasure() throws Exception {
        // Initialize the database
        measureRepository.saveAndFlush(measure);

        int databaseSizeBeforeDelete = measureRepository.findAll().size();

        // Delete the measure
        restMeasureMockMvc.perform(delete("/api/measures/{id}", measure.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Measure> measureList = measureRepository.findAll();
        assertThat(measureList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Measure.class);
        Measure measure1 = new Measure();
        measure1.setId(1L);
        Measure measure2 = new Measure();
        measure2.setId(measure1.getId());
        assertThat(measure1).isEqualTo(measure2);
        measure2.setId(2L);
        assertThat(measure1).isNotEqualTo(measure2);
        measure1.setId(null);
        assertThat(measure1).isNotEqualTo(measure2);
    }
}
