package com.thefirstlineofcode.chalk.core;

import java.util.Stack;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;

public class TaskChains implements ITask<Stanza> {
	private Stack<ITask<?>> tasks;
	
	public TaskChains() {
		tasks = new Stack<>();
	}
	
	public TaskChains add(ITask<?> task) {
		tasks.push(task);
		
		return this;
	}

	@Override
	public void trigger(IUnidirectionalStream<Stanza> stream) {
		if (tasks.isEmpty()) {
			throw new IllegalStateException("No task in chains.");
		}
		
		trigger(tasks.peek(), stream);
	}
	
	private <T extends Stanza> void trigger(ITask<T> task, IUnidirectionalStream<Stanza> stream) {
		task.trigger(new GenericStream<T>(stream));
	}
	
	private class GenericStream<T extends Stanza> implements IUnidirectionalStream<T> {
		private IUnidirectionalStream<Stanza> real;
		
		public GenericStream(IUnidirectionalStream<Stanza> real) {
			this.real = real;
		}

		@Override
		public void send(T stanza) {
			real.send(stanza);
		}

		@Override
		public void send(T stanza, int timeout) {
			real.send(stanza, timeout);
		}
		
		
	}

	@Override
	public void processResponse(IUnidirectionalStream<Stanza> stream, Stanza stanza) {
		ITask<?> task = tasks.pop();
		processResponse(task, stream, stanza);
		
		if (!tasks.isEmpty()) {
			task = tasks.peek();
			trigger(task, stream);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Stanza> void processResponse(ITask<T> task, IUnidirectionalStream<Stanza> stream, Stanza stanza) {
		task.processResponse(new GenericStream<T>(stream), (T)stanza);
	}

	@Override
	public boolean processError(IUnidirectionalStream<Stanza> stream, StanzaError error) {
		ITask<?> task = tasks.pop();
		return processError(task, stream, error);
	}

	private <T extends Stanza> boolean processError(ITask<T> task, IUnidirectionalStream<Stanza> stream, StanzaError error) {
		return task.processError(new GenericStream<T>(stream), error);
	}

	@Override
	public boolean processTimeout(IUnidirectionalStream<Stanza> stream, Stanza stanza) {
		ITask<?> task = tasks.pop();
		return processTimeout(task, stream, stanza);
	}

	@SuppressWarnings("unchecked")
	private <T extends Stanza> boolean processTimeout(ITask<T> task, IUnidirectionalStream<Stanza> stream, Stanza stanza) {
		return task.processTimeout(new GenericStream<T>(stream), (T)stanza);
	}

	@Override
	public void interrupted() {
		if (tasks.isEmpty())
			return;
		
		ITask<?> task = tasks.pop();
		interrupted(task);
	}

	private void interrupted(ITask<?> task) {
		task.interrupted();
	}

}
