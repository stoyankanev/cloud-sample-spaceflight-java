package com.sap.cloudsamples.spaceflight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.sap.cloud.cf.monitoring.client.model.Metric;
import com.sap.cloud.cf.monitroing.client.MonitoringClient;
import com.sap.cloud.cf.monitroing.client.MonitoringClientBuilder;
import com.sap.cloud.cf.monitoring.java.CustomMetricRegistry;
import com.sap.cloud.cf.monitroing.client.configuration.CFConfigurationProvider;

import com.sap.cloud.sdk.odatav2.connectivity.ODataException;
import com.sap.cloud.sdk.service.prov.api.DataSourceHandler;
import com.sap.cloud.sdk.service.prov.api.EntityData;
import com.sap.cloud.sdk.service.prov.api.EntityDataBuilder;
import com.sap.cloud.sdk.service.prov.api.ExtensionHelper;
import com.sap.cloud.sdk.service.prov.api.annotations.BeforeCreate;
import com.sap.cloud.sdk.service.prov.api.annotations.BeforeUpdate;
import com.sap.cloud.sdk.service.prov.api.exception.DatasourceException;
import com.sap.cloud.sdk.service.prov.api.exits.BeforeCreateResponse;
import com.sap.cloud.sdk.service.prov.api.exits.BeforeUpdateResponse;
import com.sap.cloud.sdk.service.prov.api.exits.PreExtensionResponseBuilderWithBody;
import com.sap.cloud.sdk.service.prov.api.exits.PreExtensionResponseImpl;
import com.sap.cloud.sdk.service.prov.api.exits.PreExtensionResponseWithBody;
import com.sap.cloud.sdk.service.prov.api.request.CreateRequest;
import com.sap.cloud.sdk.service.prov.api.request.UpdateRequest;
import com.sap.cloud.sdk.service.prov.api.response.ErrorResponse;
import com.sap.cloud.sdk.service.prov.api.response.ErrorResponseBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Request handler for <code>BookingService.Bookings</code> entity.
 */
public class BookingsHandler {

	private static final Logger logger = LoggerFactory.getLogger(BookingsHandler.class);

	static final String BOOKING_SERVICE = "BookingService";
	private static final String PROPERTY_ID = "ID";

	private static final String ENTITY_BOOKINGS = "Bookings";

	private static final String PROPERTY_BOOKING_BOOKINGNO = "BookingNo";
	private static final String PROPERTY_BOOKING_CUSTOMERID = "Customer_ID";


	private static CustomMetricRegistry metricRegistry = new CustomMetricRegistry();
	private static Counter bookingCreatedCountMetric = metricRegistry.counter("bookings-create-metric");
	private static Counter bookingUpdatedCountMetric = metricRegistry.counter("bookings-update-metric");
	
	static{
		metricRegistry.getReporter().start(20, TimeUnit.SECONDS);
		metricRegistry.getReporter().report();
	}

	/**
	 * Called before an entity instance is created
	 */
	@BeforeCreate(serviceName = BOOKING_SERVICE, entity = ENTITY_BOOKINGS)
	public BeforeCreateResponse beforeBookingCreate(CreateRequest req, ExtensionHelper helper)
			throws ODataException, DatasourceException {
		bookingCreatedCountMetric.inc();
		logger.info("Create a new booking for customer");
		return beforeUpsert(req.getData(), helper.getHandler(), true);
	}

	/**
	 * Called before an entity instance is updated
	 */
	@BeforeUpdate(serviceName = BOOKING_SERVICE, entity = ENTITY_BOOKINGS)
	public BeforeUpdateResponse beforeBookingUpdate(UpdateRequest req, ExtensionHelper helper)
			throws ODataException, DatasourceException {
		bookingUpdatedCountMetric.inc();
		logger.info("Update booking for customer");

		return beforeUpsert(req.getData(), helper.getHandler(), false);
	}

	/**
	 * Handles BeforeCreate and BeforeUpdate of Bookings
	 */
	private PreExtensionResponseWithBody beforeUpsert(EntityData reqData, DataSourceHandler dataSource, boolean insert)
			throws ODataException, DatasourceException {
		ErrorResponseBuilder errorResponseBuilder = ErrorResponse.getBuilder();
		boolean success = true;
		EntityDataBuilder entityBuilder = EntityData.getBuilder(reqData);

		// get the booking's customer from remote, and store it in the local DB
		// success &= fetchAndSaveCustomer(reqData, dataSource, errorResponseBuilder);

		if (!success) {
			return new PreExtensionResponseImpl(
					errorResponseBuilder.setStatusCode(HttpStatus.SC_BAD_REQUEST).response());
		}

		if (insert) {
			// compute and set a new booking number
			entityBuilder.addElement(PROPERTY_BOOKING_BOOKINGNO, computeBookingNumber());
		}

		return new PreExtensionResponseBuilderWithBody(entityBuilder.buildEntityData(ENTITY_BOOKINGS)).response();
	}

	/**
	 * Retrieves the booking's customer from remote, and stores it in the local DB
	 * 
	 * @return <code>false</code> in case of errors
	 */
	// private static boolean fetchAndSaveCustomer(EntityData reqData,
	// DataSourceHandler dataSource,
	// ErrorResponseBuilder errorResponseBuilder) {
	// if (reqData.contains(PROPERTY_BOOKING_CUSTOMERID)) {
	// String custId =
	// String.valueOf(reqData.getElementValue(PROPERTY_BOOKING_CUSTOMERID));
	// try {
	// Customer customer = CustomersReplicator.fetchCustomer(custId, true);
	// CustomersReplicator.saveCustomer(customer, dataSource);
	// } catch (Exception e) {
	// logger.error(e.getMessage(), e);
	// addErrorMessage(errorResponseBuilder, PROPERTY_BOOKING_CUSTOMERID,
	// "NoSuchCustomer", custId);
	// return false;
	// }
	// }
	// return true;
	// }

	private static void addErrorMessage(ErrorResponseBuilder responseBuilder, String target, String messageKey,
			Object... messageArgs) {
		responseBuilder.setMessage(messageKey, messageArgs).addErrorDetail(messageKey, target, messageArgs);
	}

	private static String computeBookingNumber() {
		String no = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now()); // e.g. 20180810
		no += "/" + RandomStringUtils.randomAlphanumeric(5).toUpperCase(Locale.ENGLISH); // short random string
		return no;
	}

}