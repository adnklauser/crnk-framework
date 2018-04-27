package io.crnk.core.queryspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.ParagraphView;

import org.junit.Before;

import com.sun.javafx.scene.control.skin.VirtualFlow;

import io.crnk.core.CoreTestContainer;
import io.crnk.core.engine.information.InformationBuilder;
import io.crnk.core.engine.information.contributor.ResourceFieldContributor;
import io.crnk.core.engine.information.contributor.ResourceFieldContributorContext;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceFieldAccess;
import io.crnk.core.engine.information.resource.ResourceFieldAccessor;
import io.crnk.core.engine.internal.information.DefaultInformationBuilder;
import io.crnk.core.engine.parser.TypeParser;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.mock.models.Task;
import io.crnk.core.module.ModuleRegistry;
import io.crnk.core.module.SimpleModule;
import io.crnk.core.queryspec.pagingspec.CustomOffsetLimitPagingBehavior;
import io.crnk.core.queryspec.pagingspec.OffsetLimitPagingBehavior;
import io.crnk.core.queryspec.pagingspec.OffsetLimitPagingSpec;
import io.crnk.core.queryspec.pagingspec.PagingBehavior;
import io.crnk.legacy.internal.DefaultQuerySpecConverter;
import io.crnk.legacy.queryParams.DefaultQueryParamsParser;
import io.crnk.legacy.queryParams.QueryParamsBuilder;

public abstract class AbstractQuerySpecTest {

	protected DefaultQuerySpecConverter querySpecConverter;

	protected QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder(new DefaultQueryParamsParser());

	protected ResourceRegistry resourceRegistry;

	protected ModuleRegistry moduleRegistry;

	protected CoreTestContainer container;

	protected static void addParams(Map<String, Set<String>> params, String key, String value) {
		params.put(key, new HashSet<>(Arrays.asList(value)));
	}

	/**
	 * Override this method to register test-specific paging behaviors.
	 * Make sure that {@link io.crnk.core.module.discovery.ReflectionsServiceDiscovery}
	 * doesn't pick them up already (depends on the package location of the test).
	 * @return your list of paging behaviors you want to have registered too.
	 */
	protected List<PagingBehavior> additionalPagingBehaviors() {
		return new ArrayList<>();
	}

	@Before
	public void setup() {
		container = new CoreTestContainer();
		ResourceFieldContributor contributor = new ResourceFieldContributor() {
			@Override
			public List<ResourceField> getResourceFields(ResourceFieldContributorContext context) {
				List<ResourceField> fields = new ArrayList<>();
				if (context.getResourceInformation().getResourceClass() == Task.class) {
					// add additional field that is not defined on the class
					String name = "computedAttribute";
					ResourceFieldAccess access = new ResourceFieldAccess(true, true, true, true, true);

					InformationBuilder informationBuilder = new DefaultInformationBuilder(new TypeParser());

					InformationBuilder.Field fieldBuilder = informationBuilder.createResourceField();
					fieldBuilder.type(Integer.class);
					fieldBuilder.jsonName(name);
					fieldBuilder.underlyingName(name);
					fieldBuilder.access(access);
					fieldBuilder.accessor(new ResourceFieldAccessor() {

						public Object getValue(Object resource) {
							return 13;
						}

						public void setValue(Object resource, Object fieldValue) {

						}
					});
					fields.add(fieldBuilder.build());

				}
				return fields;
			}
		};
		SimpleModule module = new SimpleModule("test");
		module.addResourceFieldContributor(contributor);

		// depending on the actual test run, the paging behaviors may or may NOT be picked up by
		// ReflectionsServiceDiscovery (bad enough!). OffsetLimitPagingBehavior should
		// always be registered anyhow. CustomOffsetLimitPagingBehavior should be added
		// explicitly where needed. See additionalPagingBehaviors().
		// module.addPagingBehavior(new OffsetLimitPagingBehavior());
		// module.addPagingBehavior(new CustomOffsetLimitPagingBehavior());

		additionalPagingBehaviors().forEach(module::addPagingBehavior);

		setup(container);
		container.addModule(module);
		container.boot();

		moduleRegistry = container.getModuleRegistry();
		querySpecConverter = new DefaultQuerySpecConverter(moduleRegistry);
		resourceRegistry = container.getResourceRegistry();
	}

	protected void setup(CoreTestContainer container) {
		container.setPackage(getResourceSearchPackage());
	}

	public String getResourceSearchPackage() {
		return getClass().getPackage().getName();
	}

	protected QuerySpec querySpec(Long offset, Long limit) {
		QuerySpec querySpec = new QuerySpec(Task.class);
		querySpec.setPagingSpec(new OffsetLimitPagingSpec(offset, limit));
		return querySpec;
	}

	protected QuerySpec querySpec() {
		return querySpec(0L, null);
	}
}
