/**
 * @typedef {Object} UserInfo
 * @property {string} name
 * @property {string} studentId
 * @property {string} department
 * @property {string} [avatar]
 * @property {string} [signature]
 */

/**
 * @typedef {'pending'|'processing'|'done'|'cancelled'} RepairStatus
 * @typedef {'high'|'medium'|'low'} UrgencyLevel
 */

/**
 * @typedef {Object} RepairRecord
 * @property {number} id
 * @property {string} location
 * @property {string} description
 * @property {UrgencyLevel} urgency
 * @property {RepairStatus} status
 * @property {string} [image]
 * @property {string} date
 */

/**
 * @typedef {Object} ApiResponse<T>
 * @property {number} code
 * @property {T} data
 * @property {string} [message]
 */

export default {}
