/*
 * Copyright (C) 2015 Arthur Gregorio, AG.Software
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.com.webbudget.domain.service;

import br.com.webbudget.infraestructure.MessagesFactory;
import br.com.webbudget.domain.entity.movement.CostCenter;
import br.com.webbudget.domain.entity.movement.FinancialPeriod;
import br.com.webbudget.domain.entity.movement.Movement;
import br.com.webbudget.domain.entity.movement.MovementClass;
import br.com.webbudget.domain.entity.movement.MovementClassType;
import br.com.webbudget.domain.entity.movement.MovementStateType;
import br.com.webbudget.domain.entity.movement.MovementType;
import br.com.webbudget.domain.repository.movement.ICostCenterRepository;
import br.com.webbudget.domain.repository.movement.IFinancialPeriodRepository;
import br.com.webbudget.domain.repository.movement.IMovementRepository;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartSeries;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.PieChartModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Arthur Gregorio
 *
 * @version 1.0.0
 * @since 1.0.0, 13/04/2014
 */
@Service
public class GraphModelService implements Serializable {

    @Autowired
    private MessagesFactory messages;
    @Autowired
    private IMovementRepository movementRepository;
    @Autowired
    private ICostCenterRepository costCenterRepository;
    @Autowired
    private IFinancialPeriodRepository financialPeriodRepository;

    /**
     *
     * @return
     */
    @Transactional(readOnly = true)
    public PieChartModel buildExpensesModelByCostCenter() {

        final List<CostCenter> costCenters
                = this.totalizeCostCentersByDirection(MovementClassType.OUT);

        // total geral, todos o movimentos em todos os centros de custo
        // usado depois para calcular a porcentagem que representa o valor total
        // daquele centro de custo no total geral
        BigDecimal total = BigDecimal.ZERO;

        for (CostCenter costCenter : costCenters) {
            total = total.add(costCenter.getTotalMovements());
        }

        final PieChartModel model = new PieChartModel();

        // calcula as porcentagens para cada CC
        for (CostCenter costCenter : costCenters) {
            if (costCenter.getTotalMovements() != BigDecimal.ZERO) {

                final BigDecimal divider = costCenter.getTotalMovements()
                        .multiply(new BigDecimal("100"));

                costCenter.setPercentage(divider.divide(total, RoundingMode.CEILING));

                model.set(costCenter.getName(), costCenter.getPercentage());
            }
        }

        // monta o grafico
        model.setShadow(false);
        model.setLegendCols(3);
        model.setLegendRows(10);
        model.setLegendPosition("e");
        model.setShowDataLabels(true);

        return model;
    }

    /**
     *
     * @return
     */
    @Transactional(readOnly = true)
    public PieChartModel buildRevenueModelByCostCenter() {

        final List<CostCenter> costCenters
                = this.totalizeCostCentersByDirection(MovementClassType.IN);

        // total geral, todos o movimentos em todos os centros de custo
        // usado depois para calcular a porcentagem que representa o valor total
        // daquele centro de custo no total geral
        BigDecimal total = BigDecimal.ZERO;

        for (CostCenter costCenter : costCenters) {
            total = total.add(costCenter.getTotalMovements());
        }

        final PieChartModel model = new PieChartModel();

        // calcula as porcentagens para cada CC
        for (CostCenter costCenter : costCenters) {
            if (costCenter.getTotalMovements() != BigDecimal.ZERO) {

                final BigDecimal divider = costCenter.getTotalMovements()
                        .multiply(new BigDecimal("100"));

                costCenter.setPercentage(divider.divide(total, RoundingMode.CEILING));

                model.set(costCenter.getName(), costCenter.getPercentage());
            }
        }

        // monta o grafico
        model.setShadow(false);
        model.setLegendCols(3);
        model.setLegendRows(10);
        model.setLegendPosition("e");
        model.setShowDataLabels(true);

        return model;
    }

    /**
     *
     * @param classes
     * @return
     */
    public CartesianChartModel buildClassesChartModel(List<MovementClass> classes) {

        final CartesianChartModel model = new HorizontalBarChartModel();

        final ChartSeries classesBar = this.buildClassesBar(classes);
        final ChartSeries budgetLine = this.buildBudgetLine(classes);

        model.addSeries(classesBar);
        model.addSeries(budgetLine);

        model.setAnimate(true);
        model.setExtender("extendLegend");
        model.setMouseoverHighlight(false);
        model.setDatatipFormat("<span>R$ %s</span><span style='display:none;'>%s</span>");

        this.formatGraph(classes, model);

        return model;
    }

    /**
     *
     * @param classes
     * @param model
     */
    private void formatGraph(List<MovementClass> classes, CartesianChartModel model) {

        final Axis xAxis = model.getAxis(AxisType.X);

        xAxis.setMin(0);

        BigDecimal xAxisMax = BigDecimal.ZERO;

        for (MovementClass movementClass : classes) {

            BigDecimal max = movementClass.getBudget();
            final BigDecimal maxMovement = movementClass.getTotalMovements();

            if (maxMovement != null) {
                if (max.compareTo(maxMovement) < 0) {
                    max = maxMovement;
                }
            }

            if (max.compareTo(xAxisMax) > 0) {
                xAxisMax = max;
            }
        }

        xAxisMax = xAxisMax.add(new BigDecimal("100"));

        xAxis.setMax(xAxisMax);
    }

    /**
     *
     * @param classes
     * @return
     */
    private ChartSeries buildClassesBar(List<MovementClass> classes) {

        final ChartSeries series = new BarChartSeries();

        series.setLabel(this.messages.getMessage("period-details.chart.classes"));

        for (MovementClass movementClass : classes) {
            series.set(movementClass.getName(), movementClass.getTotalMovements());
        }

        return series;
    }

    /**
     *
     * @param classes
     * @return
     */
    private ChartSeries buildBudgetLine(List<MovementClass> classes) {

        final ChartSeries series = new LineChartSeries();

        series.setLabel(this.messages.getMessage("period-details.chart.budget-line"));

        for (MovementClass movementClass : classes) {
            series.set(movementClass.getName(), movementClass.getBudget());
        }

        return series;
    }

    /**
     * Neste metodo para cada centro de custo, calculamos o valor de movimentos
     * pagos nos periodos abertos para mostrar na home
     *
     * @return a lista de centros de custo com seus valores de movimentos
     */
    private List<CostCenter> totalizeCostCentersByDirection(MovementClassType direction) {

        final List<CostCenter> costCenters
                = this.costCenterRepository.listByStatus(false);

        final List<FinancialPeriod> periods
                = this.financialPeriodRepository.listOpen();

        // para cada centro de custo
        for (CostCenter costCenter : costCenters) {

            BigDecimal total = BigDecimal.ZERO;

            // pegamos os periodos abertos
            for (FinancialPeriod period : periods) {

                final List<Movement> movements = this.movementRepository
                        .listByPeriodAndCostCenterAndDirection(period, costCenter, direction);

                // pegamos os movimentos
                for (Movement movement : movements) {

                    // se estiver pago e nao for fatura, soma no total
                    if (movement.getMovementStateType() == MovementStateType.PAID
                            && movement.getMovementType() == MovementType.MOVEMENT) {
                        total = total.add(movement.getValue());
                    }
                }
            }
            costCenter.setTotalMovements(total);
        }

        return costCenters;
    }
}
