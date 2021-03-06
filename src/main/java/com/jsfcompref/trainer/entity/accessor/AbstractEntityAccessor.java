
package com.jsfcompref.trainer.entity.accessor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnit;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

/**
 *
 * @author Dr. Spock (spock at dev.java.net)
 */
public abstract class AbstractEntityAccessor {

    @PersistenceUnit
    private EntityManagerFactory emf;
    @Resource
    private UserTransaction userTransaction;

    private AtomicInteger count;

    public AbstractEntityAccessor() {
        count = new AtomicInteger(0);
    }

    protected final <T> T doInTransaction(PersistenceAction<T> action) throws EntityAccessorException {
        EntityManager em = emf.createEntityManager();
        try {
            int status = 0;
            if (Status.STATUS_ACTIVE != (status = userTransaction.getStatus())){
                count.incrementAndGet();
                userTransaction.begin();
            }
            T result = action.execute(em);
            if (Status.STATUS_ACTIVE == (status = userTransaction.getStatus())) {
                if (0 == count.decrementAndGet()) {
                    userTransaction.commit();
                }
            }
            return result;
        } catch (Exception e) {
            try {
                userTransaction.rollback();
            } catch (Exception ex) {
                Logger.getLogger(AbstractEntityAccessor.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new EntityAccessorException(e);
        } finally {
            em.close();
        }

    }

    protected final void doInTransaction(PersistenceActionWithoutResult action) throws EntityAccessorException {
        EntityManager em = emf.createEntityManager();
        try {
            int status = 0;
            if (Status.STATUS_ACTIVE != (status = userTransaction.getStatus())){
                count.incrementAndGet();
                userTransaction.begin();
            }
            action.execute(em);
            if (Status.STATUS_ACTIVE == (status = userTransaction.getStatus())) {
                if (0 == count.decrementAndGet()) {
                    userTransaction.commit();
                }
            }
        } catch (Exception e) {
            try {
                userTransaction.rollback();
            } catch (Exception ex) {
                Logger.getLogger(AbstractEntityAccessor.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new EntityAccessorException(e);
        } finally {
            em.close();
        }
    }

    protected static interface PersistenceAction<T> {

        T execute(EntityManager em);
    }

    protected static interface PersistenceActionWithoutResult {

        void execute(EntityManager em);
    }

}
