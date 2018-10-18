/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.Map;

import com.google.cloud.datastore.BaseEntity;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.ConversionException;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.TypeInformation;

/**
 * A {@link PropertyValueProvider} for Datastore entities
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class EntityPropertyValueProvider implements PropertyValueProvider<DatastorePersistentProperty> {
	private final BaseEntity entity;

	private final ReadWriteConversions conversion;


	public EntityPropertyValueProvider(BaseEntity entity, ReadWriteConversions readWriteConversions) {
		if (entity == null) {
			throw new DatastoreDataException("A non-null entity is required");
		}
		this.entity = entity;
		this.conversion = readWriteConversions;
	}

	@Override
	public <T> T getPropertyValue(DatastorePersistentProperty persistentProperty) {
		Class singularType = persistentProperty.getComponentType();
		Class collectionType = persistentProperty.getType();
		if (singularType == null) {
			singularType = collectionType;
			collectionType = null;
		}
		return getPropertyValue(persistentProperty.getFieldName(),
				persistentProperty.isEmbedded(),
				Map.class.isAssignableFrom(persistentProperty.getType()),
				persistentProperty.getEmbeddedMapValueType(), collectionType,
				singularType);
	}

	/**
	 * Get a property value from the entity.
	 * @param fieldName the name of the field to get.
	 * @param isEmbedded if the property is an embedded entity.
	 * @param isEmbeddedMap if the property should be retrieved as a map representing the
	 * entity value
	 * @param embeddedMapValueType if the property is an embedded entity and also to be
	 * read as a {@code Map}, the value type of the map.
	 * @param collectionType the collection type if the property is not singular. null if
	 * the property is singular.
	 * @param componentType the singular item type.
	 * @return the property converted from the entity.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getPropertyValue(String fieldName, boolean isEmbedded,
			boolean isEmbeddedMap, TypeInformation embeddedMapValueType,
			Class collectionType, Class componentType) {
		if (!this.entity.contains(fieldName)) {
			return null;
		}
		try {
			if (isEmbedded) {
				return isEmbeddedMap
						? this.conversion.convertOnReadEmbeddedMap(
								this.entity.getEntity(fieldName), embeddedMapValueType)
						: this.conversion.convertOnReadEmbedded(
								this.entity.getValue(fieldName).get(), collectionType,
								componentType);
			}
			else {
				return this.conversion.convertOnRead(
						this.entity.getValue(fieldName).get(), collectionType,
						componentType);
			}
		}
		catch (ConversionException | DatastoreDataException e) {
			throw new DatastoreDataException("Unable to read property " + fieldName, e);
		}
	}
}
