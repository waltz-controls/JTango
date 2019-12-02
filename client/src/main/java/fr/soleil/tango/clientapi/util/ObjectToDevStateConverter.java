package fr.soleil.tango.clientapi.util;

import fr.esrf.Tango.DevState;
import net.entropysoft.transmorph.ConversionContext;
import net.entropysoft.transmorph.ConverterException;
import net.entropysoft.transmorph.IConverter;
import net.entropysoft.transmorph.type.TypeReference;
import org.tango.DeviceState;

public final class ObjectToDevStateConverter implements IConverter {
    @Override
    public Object convert(final ConversionContext context, final Object sourceObject,
                          final TypeReference<?> destinationType) throws ConverterException {
        if (sourceObject == null) {
            if (destinationType.isPrimitive()) {
                throw new ConverterException("Cannot convert null to primitive number");
            } else {
                return null;
            }
        }
        Object result;
        if (sourceObject instanceof String) {
            final String state = (String) sourceObject;
            result = DeviceState.toDevState(state);
        } else {
            final Number state = (Number) sourceObject;
            result = DevState.from_int(state.intValue());
        }
        return result;
    }

    @Override
    public boolean canHandle(final ConversionContext context, final Object sourceObject,
                             final TypeReference<?> destinationType) {
        boolean canHandle = false;
        if ((sourceObject instanceof String || sourceObject instanceof Number)
                && destinationType.isType(DevState.class)) {
            canHandle = true;
        }
        return canHandle;
    }
}
