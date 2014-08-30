/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.convert;

import java.util.Map;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.BiMap;

public class ClassNamer
{
	public interface SidedClassNamer
	{
		String getName( String name );
	}
	
	private Map<String,String> m_sourceNames;
	private Map<String,String> m_destNames;
	
	public ClassNamer( BiMap<ClassIdentity,ClassIdentity> mappings )
	{
		// convert the identity mappings to name maps
		m_sourceNames = Maps.newHashMap();
		m_destNames = Maps.newHashMap();
		int i = 0;
		for( Map.Entry<ClassIdentity,ClassIdentity> entry : mappings.entrySet() )
		{
			String name = String.format( "M%04d", i++ );
			m_sourceNames.put( entry.getKey().getClassEntry().getName(), name );
			m_destNames.put( entry.getValue().getClassEntry().getName(), name );
		}
	}
	
	public String getSourceName( String name )
	{
		return m_sourceNames.get( name );
	}
	
	public String getDestName( String name )
	{
		return m_destNames.get( name );
	}
	
	public SidedClassNamer getSourceNamer( )
	{
		return new SidedClassNamer( )
		{
			@Override
			public String getName( String name )
			{
				return getSourceName( name );
			}
		};
	}
	
	public SidedClassNamer getDestNamer( )
	{
		return new SidedClassNamer( )
		{
			@Override
			public String getName( String name )
			{
				return getDestName( name );
			}
		};
	}
}
