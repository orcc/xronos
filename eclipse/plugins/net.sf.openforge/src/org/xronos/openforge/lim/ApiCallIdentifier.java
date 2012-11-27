/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package org.xronos.openforge.lim;

import java.util.ArrayList;
import java.util.List;

/**
 * An ApiCallIdentifier identifies a {@link Call Call} to be a Forged
 * (forgeable) API method call. These special API methods contains constant
 * information set by the users that says about the enclosing methods. Each
 * ApiCallIdentifier has an identifier call a {@link ApiCallIdentifier.Tag Tag}.
 * A {@link ApiCallIdentifier.Tag Tag} consists of an ApiCallIdentifier type,
 * {@link ApiCallIdentifier#THROUGHPUT_LOCAL}.
 * 
 * <p>
 * <b>NOTE: Please add new Types if we have additional forgeable api
 * methods.</b>
 * 
 * @author ysyu
 * @version $Id: ApiCallIdentifier.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ApiCallIdentifier {

	/* The identifier tag of api call */
	private ApiCallIdentifier.Tag tag;

	/* the call to which this identifier belongs */
	private Call owner;

	/*
	 * the collection of constant api call specifications.
	 * 
	 * NOTE: now, these are just ordered constants from this ApiCallIdentifier
	 * owner's method parameters
	 */
	private List<Number> specifications = new ArrayList<Number>();

	/**
	 * A type identifier for an ApiCallIdentifier.
	 * 
	 * @version $Id: ApiCallIdentifier.java 2 2005-06-09 20:00:48Z imiller $
	 */
	public final static class Type {

		static final int ID_THROUGHPUT_LOCAL = 0;

		/* the kind of ApiCallIdentifier */
		private int id;

		private Type(int id) {
			this.id = id;
		}

		private int getId() {
			return id;
		}

		@Override
		public String toString() {
			switch (getId()) {
			case ID_THROUGHPUT_LOCAL:
				return "THROUGHPUT LOCAL";
			default:
				assert false : "Unknown id: " + getId();
				return null;
			}
		}
	}

	/*
	 * The type of ApiCallIdentifier that represents the throughput local go
	 * spacing
	 */
	public static final Type THROUGHPUT_LOCAL = new Type(
			Type.ID_THROUGHPUT_LOCAL);

	/**
	 * A unique, hashable identifier for an {@link ApiCallIdentifier}
	 * 
	 * @version $Id: ApiCallIdentifier.java 2 2005-06-09 20:00:48Z imiller $
	 */
	public static class Tag {
		/* The Call type */
		private ApiCallIdentifier.Type type;

		/* the Call name */
		private String name;

		private Tag(ApiCallIdentifier.Type type, String name) {
			this.type = type;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public ApiCallIdentifier.Type getType() {
			return type;
		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Tag) {
				Tag tag = (Tag) obj;
				return tag.getType() == getType();
			}
			return false;
		}

		@Override
		public String toString() {
			return type.toString();
		}
	}

	/**
	 * @returns the tag of this identifier
	 */
	public ApiCallIdentifier.Tag getTag() {
		return tag;
	}

	/**
	 * Gets a named ApiCallIdentifier tag.
	 * 
	 * @param type
	 *            the ApiCallIdentifier type
	 * @param name
	 *            the name of the Call
	 * @return a named tag
	 */
	public static ApiCallIdentifier.Tag getTag(ApiCallIdentifier.Type type,
			String name) {
		return new Tag(type, name);
	}

	/**
	 * @param tag
	 */
	public void setTag(ApiCallIdentifier.Tag tag) {
		this.tag = tag;
	}

	/**
	 * Set the api call specification
	 * 
	 * @param spec
	 *            api call constant
	 */
	public void setSpecification(int spec) {
		specifications.add(new Integer(spec));
	}

	/**
	 * Set the api call specification
	 * 
	 * @param spec
	 *            api call constant
	 */
	public void setSpecification(Number spec) {
		specifications.add(spec);
	}

	/**
	 * Set the specification with a list of specifications
	 * 
	 * @param specs
	 *            list of api call constants
	 */
	void setSpecifications(List<Number> specs) {
		specifications.addAll(specs);
	}

	/**
	 * @return an ordered list of constant specifications
	 */
	public List<Number> getSpecifications() {
		return specifications;
	}

	/**
	 * The {@link Call} that this ApiCallIdentifier identifies
	 * 
	 * @return a {@link Call}
	 */
	public Call getOwner() {
		return owner;
	}

	/**
	 * Set the {@link Call} that this ApiCallIdentifier identities
	 * 
	 * @param owner
	 *            {@link Call} that owns this identifier
	 */
	public void setOwner(Call owner) {
		this.owner = owner;
	}

	/**
	 * @return name of this identifier
	 */
	public String getName() {
		return getTag().getName();
	}

	public ApiCallIdentifier(Call owner, String api_name) {
		this.owner = owner;

		if (api_name.compareTo("throughputLocal(int)") == 0) {
			tag = new Tag(THROUGHPUT_LOCAL, api_name);
		} else {
			assert false : "Unknown API method " + api_name;
		}
	}

	/**
	 * Copies the attributes of this ApiCallIdentifier
	 */
	void copyAttributes(ApiCallIdentifier apiId) {
		setOwner(apiId.getOwner());
		setTag(apiId.getTag());
		setSpecifications(apiId.getSpecifications());
	}
}
