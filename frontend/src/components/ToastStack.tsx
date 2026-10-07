import type { ToastMessage } from '../types'
import { Icon } from './UI'

export default function ToastStack({ messages, dismiss }: { messages: ToastMessage[]; dismiss: (id: number) => void }) {
  return <div className="toast-stack">{messages.map(message => <div className={`toast toast-${message.type}`} key={message.id}>
    <div className="toast-icon"><Icon name={message.type === 'success' ? 'shield' : message.type === 'error' ? 'alert' : 'pulse'} size={18}/></div>
    <span>{message.text}</span>
    <button onClick={() => dismiss(message.id)}><Icon name="close" size={14}/></button>
  </div>)}</div>
}
