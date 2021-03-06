/*
 * Copyright (c) 2016 Washington State Department of Transportation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package gov.wa.wsdot.apps.analytics.client.activities.events;

import com.google.web.bindery.event.shared.binder.GenericEvent;

import java.util.Date;


public class DateSubmitEvent extends GenericEvent {


    private final String dateRange;
    private final String account;
    private final Date startDate;
    private final Date endDate;

    public DateSubmitEvent(String dateRange, Date startDate, Date endDate, String account) {
        this.dateRange = dateRange;
        this.account = account;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getDateRange() {
        return this.dateRange;
    }

    public String getAccount() {
        return this.account;
    }

    public Date getStartDate(){ return this.startDate;}

    public Date getEndDate(){ return this.endDate;}
}
