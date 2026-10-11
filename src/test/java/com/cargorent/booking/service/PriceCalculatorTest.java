package com.cargorent.booking.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    private static final LocalDate D = LocalDate.of(2026, 10, 13);

    @Test
    void shortRentalHasNoDiscount() {
        PriceCalculator.Price p = PriceCalculator.calculate(new BigDecimal("1499.00"), D, D.plusDays(3));
        assertThat(p.days()).isEqualTo(3);
        assertThat(p.subtotal()).isEqualByComparingTo("4497.00");
        assertThat(p.discount()).isEqualByComparingTo("0");
        assertThat(p.total()).isEqualByComparingTo("4497.00");
    }

    @Test
    void weekLongRentalGetsTenPercentOff() {
        PriceCalculator.Price p = PriceCalculator.calculate(new BigDecimal("2000"), D, D.plusDays(7));
        assertThat(p.subtotal()).isEqualByComparingTo("14000");
        assertThat(p.discount()).isEqualByComparingTo("1400");
        assertThat(p.total()).isEqualByComparingTo("12600");
    }

    @Test
    void monthLongRentalGetsFifteenPercentOff() {
        PriceCalculator.Price p = PriceCalculator.calculate(new BigDecimal("1000"), D, D.plusDays(30));
        assertThat(p.discount()).isEqualByComparingTo("4500");
        assertThat(p.total()).isEqualByComparingTo("25500");
    }

    @Test
    void sameDayCountsAsOneDay() {
        assertThat(PriceCalculator.days(D, D)).isEqualTo(1);
    }
}
