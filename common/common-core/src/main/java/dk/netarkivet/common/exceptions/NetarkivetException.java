/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.exceptions;

/**
 * Base exception for all Netarkivet exceptions. Note that RuntimeException is extended
 */
@SuppressWarnings({"serial"})
public abstract class NetarkivetException extends RuntimeException {

    /**
     * Constructs new NetarkivetException with the specified detail message.
     *
     * @param message The detail message
     */
    public NetarkivetException(String message) {
        super(message);
    }

    /**
     * Constructs new NetarkivetException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause
     */
    public NetarkivetException(String message, Throwable cause) {
        super(message, cause);
    }
}
