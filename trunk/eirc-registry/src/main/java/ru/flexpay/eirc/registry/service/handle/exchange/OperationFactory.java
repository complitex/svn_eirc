package ru.flexpay.eirc.registry.service.handle.exchange;

import com.google.common.collect.ImmutableMap;
import org.complitex.dictionary.service.exception.AbstractException;
import ru.flexpay.eirc.registry.entity.Container;
import ru.flexpay.eirc.registry.entity.ContainerType;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.Map;

/**
 * @author Pavel Sknar
 */
@Singleton
public class OperationFactory {

    @EJB
    private OpenAccountOperation openAccountOperation;

    @EJB
    private SetHeatedSquareOperation setHeatedSquareOperation;

    @EJB
    private SetNumberOfHabitantsOperation setNumberOfHabitantsOperation;

    @EJB
    private SetLiveSquareOperation setLiveSquareOperation;

    @EJB
    private SetTotalSquareOperation setTotalSquareOperation;

    @EJB
    private SetResponsiblePerson setResponsiblePerson;

    @EJB
    private SaldoOutOperation saldoOutOperation;

    @EJB
    private ChargeOperation chargeOperation;

    @EJB
    private CloseAccountOperation closeAccountOperation;

    @EJB
    private CashPaymentOperation cashPaymentOperation;

    @EJB
    private CashlessPaymentOperation cashlessPaymentOperation;

    private Map<ContainerType, Operation> operations = null;

    public Operation getOperation(Container container) throws AbstractException {
        init();

        Operation operation = operations.get(container.getType());
        if (operation == null) {
            throw new ContainerDataException("Unknown container type: {0}", container);
        }
        return operation;
    }

    private void init() {
        if (operations == null) {
            operations = ImmutableMap.<ContainerType, Operation>builder().
                    put(ContainerType.OPEN_ACCOUNT, openAccountOperation).
                    put(ContainerType.SET_WARM_SQUARE, setHeatedSquareOperation).
                    put(ContainerType.SET_NUMBER_ON_HABITANTS, setNumberOfHabitantsOperation).
                    put(ContainerType.SET_LIVE_SQUARE, setLiveSquareOperation).
                    put(ContainerType.SET_TOTAL_SQUARE, setTotalSquareOperation).
                    put(ContainerType.SET_RESPONSIBLE_PERSON, setResponsiblePerson).
                    put(ContainerType.SALDO_OUT, saldoOutOperation).
                    put(ContainerType.CHARGE, chargeOperation).
                    put(ContainerType.CLOSE_ACCOUNT, closeAccountOperation).
                    put(ContainerType.CASH_PAYMENT, cashPaymentOperation).
                    put(ContainerType.CASHLESS_PAYMENT, cashlessPaymentOperation).
                    build();
        }
    }


}
