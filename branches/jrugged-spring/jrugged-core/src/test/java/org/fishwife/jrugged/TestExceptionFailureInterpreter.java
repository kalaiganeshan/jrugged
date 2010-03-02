/* Copyright 2009 Comcast Interactive Media, LLC.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.fishwife.jrugged;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public final class TestExceptionFailureInterpreter extends TestCase {

	private ExceptionFailureInterpreter impl;

	public void setUp() {
		impl = new ExceptionFailureInterpreter();
	}

	// constructor tests
	public void testDefaultConstructor() {
		
		assertEquals(0, impl.getFrequency());
		assertEquals(0, impl.getTime());

		assertEquals(1, impl.getTrip().size());
		assertTrue(impl.getTrip().contains(Throwable.class));

		assertEquals(0, impl.getIgnore().size());
	}

	public void testConstructorWithIgnore() {
		final Class exnClass = RuntimeException.class;
		final Class[] myIgnore =  { exnClass };

		impl = new ExceptionFailureInterpreter(myIgnore);
		
		assertEquals(0, impl.getFrequency());
		assertEquals(0, impl.getTime());

		assertEquals(1, impl.getIgnore().size());
		for(Class clazz : impl.getIgnore()) {
			assertSame(clazz, exnClass);
		}

		assertEquals(1, impl.getTrip().size());
		assertTrue(impl.getTrip().contains(Throwable.class));

	}

	public void testConstructorWithIgnoreAndWindow() {
		final Class exnClass = RuntimeException.class;
		final Class[] myIgnore =  { exnClass };
		final int frequency = 7777;
		final long time = 1234L;
		final TimeUnit unit = TimeUnit.MILLISECONDS;

		impl = new ExceptionFailureInterpreter(myIgnore, frequency, time, unit);
		
		assertEquals(frequency, impl.getFrequency());
		assertEquals(time, impl.getTime());
		assertSame(unit, impl.getUnit());

		assertEquals(1, impl.getIgnore().size());
		for(Class clazz : impl.getIgnore()) {
			assertSame(clazz, exnClass);
		}

		assertEquals(1, impl.getTrip().size());
		assertTrue(impl.getTrip().contains(Throwable.class));
	}

	public void testConstructorWithIgnoreAndTrip() {
		final Class ignoreClass = RuntimeException.class;
		final Class[] myIgnore =  { ignoreClass };
		final Class tripClass = IOException.class;
		final Class[] myTrip = { tripClass };

		impl = new ExceptionFailureInterpreter(myIgnore, myTrip);
		
		assertEquals(0, impl.getFrequency());
		assertEquals(0, impl.getTime());

		assertEquals(1, impl.getIgnore().size());
		for(Class clazz : impl.getIgnore()) {
			assertSame(clazz, ignoreClass);
		}
		assertEquals(1, impl.getTrip().size());
		for(Class clazz : impl.getTrip()) {
			assertSame(clazz, tripClass);
		}
	}

	public void testConstructorWithIgnoreAndTripAndTolerance() {
		final Class ignoreClass = RuntimeException.class;
		final Class[] myIgnore =  { ignoreClass };
		final Class tripClass = IOException.class;
		final Class[] myTrip = { tripClass };
		final int frequency = 7777;
		final long time = 1234L;
		final TimeUnit unit = TimeUnit.MILLISECONDS;

		impl = new ExceptionFailureInterpreter(myIgnore, myTrip, frequency,
											   time, unit);
		
		assertEquals(frequency, impl.getFrequency());
		assertEquals(time, impl.getTime());
		assertSame(unit, impl.getUnit());

		assertEquals(1, impl.getIgnore().size());
		for(Class clazz : impl.getIgnore()) {
			assertSame(clazz, ignoreClass);
		}
		assertEquals(1, impl.getTrip().size());
		for(Class clazz : impl.getTrip()) {
			assertSame(clazz, tripClass);
		}
	}

	public void testIgnoredExceptionDoesNotTrip() {
		final Class ignoreClass = IOException.class;
        final Class[] myIgnore = { ignoreClass };

        impl.setIgnore(myIgnore);
        assertFalse(impl.shouldTrip(new IOException()));
	}

	public void testAnyExceptionTripsByDefault() {
        assertTrue(impl.shouldTrip(new IOException()));
	}

	public void testConfiguredTrippingExceptionsActuallyTrip() {
		final Class ignoreClass = RuntimeException.class;
        final Class[] myIgnore = { ignoreClass };
		final Class tripClass = IOException.class;
		final Class[] myTrip = { tripClass };

        impl.setIgnore(myIgnore);
		impl.setTrip(myTrip);
        assertTrue(impl.shouldTrip(new IOException()));
	}

	public void testComplainsIfTrippingExceptionsAreSubtypesOfIgnoredExceptions() {
		final Class[] myIgnore = { RuntimeException.class };
		final Class[] myTrip = { IllegalArgumentException.class };

		try { 
			impl = new ExceptionFailureInterpreter(myIgnore, myTrip);
			fail("should have complained");
		} catch (Exception expected) {
		}
	}

	public void testAllowsIgnoredExceptionsToBeSubtypesOfTrippingExceptions() {
		final Class[] myIgnore = { IllegalArgumentException.class };
		final Class[] myTrip = { RuntimeException.class };

		try { 
			impl = new ExceptionFailureInterpreter(myIgnore, myTrip);
		} catch (Exception bogosity) {
			fail("should have let me do this");
		}
	}

	public void testDoesntTripIfFailuresAreWithinTolerance() {
		impl.setFrequency(2);
		impl.setTime(1);
		impl.setUnit(TimeUnit.SECONDS);
		Exception exn1 = new Exception();
		Exception exn2 = new Exception();
		boolean result = impl.shouldTrip(exn1);
		assertFalse("this should be false 1",result);
		result = impl.shouldTrip(exn2);
		assertFalse("this should be false 2",result);
	}

	public void testTripsIfFailuresExceedTolerance() {
		impl.setFrequency(2);
		impl.setTime(1);
		impl.setUnit(TimeUnit.SECONDS);
		assertFalse("this should be false 1",impl.shouldTrip(new Exception()));
		assertFalse("this should be false 2",impl.shouldTrip(new Exception()));
		assertTrue("this should be true 3",impl.shouldTrip(new Exception()));
	}
    
}